package org.angryscan.app.scan

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.db.DatabaseConnector
import org.angryscan.app.db.models.*
import org.angryscan.app.scan.common.connectors.IDatabaseConnector
import org.angryscan.app.scan.common.connectors.IFileConnector
import org.angryscan.app.scan.common.files.extensions.requireKeywords
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.app.scan.engine.EngineChainCache
import org.angryscan.app.scan.engine.EngineChainKey
import org.angryscan.app.scan.engine.buildEngineChain
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

class ScanThread : KoinComponent {
    private val scanThreadScope = CoroutineScope(Dispatchers.Default)

    private val database: DatabaseConnector by inject()

    private val tasks: TasksViewModel by inject()

    private val scanningFileId: AtomicInteger = AtomicInteger(-1)

    private val _started = AtomicBoolean(false)
    val started: Boolean get() = _started.get()
    private val stopRequested = AtomicBoolean(false)

    private var retryCount = 0

    private val engineCache = EngineChainCache()

    private val taskMetadataCache = mutableMapOf<Int, TaskMetadata>()

    suspend fun stop() {
        logger.debug { "Stop requested for scan thread [$scanThreadScope]." }
        stopRequested.set(true)

        scanThreadScope.launch {
            while (_started.get())
                delay(1000.milliseconds)
            logger.debug { "Scan thread [$scanThreadScope] stopped by request." }
        }.join()
    }

    @OptIn(ExperimentalTime::class)
    fun start() {
        logger.debug { "Starting scan thread [$scanThreadScope]." }
        _started.set(true)
        scanThreadScope.launch {
            val scanSettings = inject<ScanSettings>()
            try {
                while (_started.get() && !stopRequested.get()) {
                    yield()
                    val tasksToScan = tasks.tasks.value.filter { it.state.value == TaskState.SCANNING }
                    if (tasksToScan.isEmpty()) {
                        retryCount++
                        if (retryCount > 3) {
                            retryCount = 0
                            logger.debug { "Nothing to scan. Scan thread [$scanThreadScope] stopped." }
                            break
                        }
                        engineCache.evictStale()
                        delay(1000.milliseconds)
                        continue
                    }

                    retryCount = 0

                    val taskEntity = tasksToScan.random()
                    val taskId = taskEntity.dbTask.id.value

                    val cachedMeta = taskMetadataCache[taskId]

                    val (dbFile, freshMeta) = claimFileAndFetchMetadata(taskEntity, cachedMeta)

                    if (freshMeta != null) {
                        taskMetadataCache[taskId] = freshMeta
                    }

                    if (dbFile == null) {
                        scanningFileId.set(-1)

                        val rescuedPending = database.transaction {
                            TaskFiles.update(
                                where = {
                                    TaskFiles.task.eq(taskEntity.dbTask.id) and
                                        TaskFiles.state.eq(TaskState.PENDING)
                                }
                            ) {
                                it[state] = TaskState.SEARCHING
                            }
                        }

                        if (rescuedPending > 0) {
                            logger.debug { "Rescued $rescuedPending PENDING files for task ${taskEntity.id.value}" }
                            continue
                        }

                        taskEntity.checkProgress()

                        retryCount++
                        if (retryCount > 3) {
                            retryCount = 0
                            break
                        }
                        delay(1000.milliseconds)
                        continue
                    }

                    val meta = taskMetadataCache[taskId]!!
                    val fileId = dbFile[TaskFiles.id].value
                    val filePath = dbFile[TaskFiles.path]
                    val connector = taskEntity.dbTask.connector
                    val primaryEngineClass = scanSettings.value.engine.value

                    when (connector) {
                        is IFileConnector -> {
                            val fileObject = connector.getFile(filePath)
                            val fileTypes = IFileType
                                .getFileType(fileObject)
                                .filter { ft -> ft in meta.extensions }

                            val requireKeywords = fileTypes.requireKeywords(fileObject.extension)
                            val engines = cachedEngineChain(primaryEngineClass, meta, requireKeywords)

                            scanningFileId.set(fileId)

                            val timer = measureTimeMillis {
                                scanFile(fileObject, fileId, fileTypes, engines, meta, taskEntity)
                            }
                            logger.debug {
                                "Scanned file with extension ${fileObject.extension} and size ${fileObject.length()} in $timer ms"
                            }
                        }

                        is IDatabaseConnector -> {
                            val structuredRows = connector.getTableContentStructured(filePath)
                            val engines = cachedEngineChain(primaryEngineClass, meta, requireKeywords = false)

                            scanningFileId.set(fileId)

                            val timer = measureTimeMillis {
                                scanDatabaseTable(filePath, fileId, structuredRows, engines, meta, taskEntity)
                            }
                            logger.debug {
                                "Scanned database table $filePath with ${structuredRows.size} rows in $timer ms"
                            }
                        }

                        else -> {
                            scanningFileId.set(fileId)
                            logger.warn { "Unsupported connector for scan task ${taskEntity.id.value}" }
                            database.transaction {
                                TaskFiles.update(where = { TaskFiles.id.eq(fileId) }) {
                                    it[state] = TaskState.FAILED
                                }
                            }
                        }
                    }

                    taskEntity.checkProgress()
                    evictStaleTasks()
                    yield()
                }
            } finally {
                engineCache.closeAll()
                taskMetadataCache.clear()
                _started.set(false)
                stopRequested.set(false)
            }
        }
    }

    private fun cachedEngineChain(
        primaryEngineClass: KClass<out IScanEngine>,
        meta: TaskMetadata,
        requireKeywords: Boolean
    ): List<IScanEngine> {
        val matchers = meta.matchers.keys.toList()
        val key = EngineChainKey.of(primaryEngineClass, matchers, requireKeywords)
        return engineCache.getOrCreate(key) {
            buildEngineChain(primaryEngineClass, matchers, requireKeywords)
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun claimFileAndFetchMetadata(
        taskEntity: TaskEntityViewModel,
        cachedMeta: TaskMetadata?
    ): Pair<org.jetbrains.exposed.sql.ResultRow?, TaskMetadata?> {
        return database.transaction {
            val resultRow = TaskFiles.selectAll()
                .where {
                    TaskFiles.task.eq(taskEntity.dbTask.id) and
                        TaskFiles.state.eq(TaskState.SEARCHING)
                }
                .limit(1)
                .firstOrNull()

            if (resultRow != null) {
                TaskFiles.update(
                    where = { TaskFiles.id.eq(resultRow[TaskFiles.id]) }
                ) {
                    it[state] = TaskState.SCANNING
                }
                taskEntity.dbTask.lastFileDate =
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }

            val meta = if (cachedMeta == null) {
                TaskMetadata(
                    fastScan = taskEntity.dbTask.fastScan,
                    matchers = TaskMatchers
                        .select(TaskMatchers.matcher, TaskMatchers.id)
                        .where { TaskMatchers.task.eq(taskEntity.dbTask.id) }
                        .associate { it[TaskMatchers.matcher] to it[TaskMatchers.id].value },
                    extensions = TaskFileExtensions
                        .select(TaskFileExtensions.extension)
                        .where { TaskFileExtensions.task.eq(taskEntity.dbTask.id) }
                        .map { it[TaskFileExtensions.extension] }
                )
            } else null

            resultRow to meta
        }
    }

    private suspend fun scanFile(
        fileObject: java.io.File,
        fileId: Int,
        fileTypes: List<IFileType>,
        engines: List<IScanEngine>,
        meta: TaskMetadata,
        taskEntity: TaskEntityViewModel
    ) {
        val scanResults = fileTypes.map { ft ->
            ft.scanFile(
                file = fileObject,
                context = currentCoroutineContext(),
                engines = engines,
                fastScan = meta.fastScan,
                selectedExtensions = meta.extensions
            )
        }

        val scanRes = scanResults.firstOrNull()
        if (scanRes != null) {
            scanResults.drop(1).forEach {
                scanRes.plus(it.getDocumentFields())
            }
        }

        if (scanRes != null && !scanRes.skipped()) {
            database.transaction {
                scanRes.getDocumentFields().forEach { field ->
                    TaskFileScanResults.insert {
                        it[file] = fileId
                        it[matcher] = meta.matchers[field.key] ?: 0
                        it[count] = field.value
                    }
                    taskEntity.addFoundAttribute(field.key, field.value)
                }
                if (!scanRes.isEmpty()) {
                    taskEntity.incrementFoundFiles()
                }
                TaskFiles.update(
                    where = { TaskFiles.id.eq(fileId) }
                ) {
                    it[state] = TaskState.COMPLETED
                }
            }
        } else {
            database.transaction {
                TaskFiles.update(
                    where = { TaskFiles.id.eq(fileId) }
                ) {
                    it[state] = TaskState.FAILED
                }
            }
        }
    }

    private suspend fun scanDatabaseTable(
        filePath: String,
        fileId: Int,
        rows: List<Map<String, String>>,
        engines: List<IScanEngine>,
        meta: TaskMetadata,
        taskEntity: TaskEntityViewModel
    ) {
        val scanRes = DatabaseContentScanner.scanContentStructured(
            path = filePath,
            rows = rows,
            engines = engines
        )

        if (!scanRes.skipped()) {
            database.transaction {
                scanRes.columnFields.forEach { (columnName, fields) ->
                    fields.forEach { (matcher, cnt) ->
                        TaskFileScanResults.insert {
                            it[file] = fileId
                            it[TaskFileScanResults.matcher] = meta.matchers[matcher] ?: 0
                            it[count] = cnt
                            it[TaskFileScanResults.columnName] = columnName
                        }
                    }
                }
                scanRes.getDocumentFields().forEach { (matcher, count) ->
                    taskEntity.addFoundAttribute(matcher, count)
                }
                if (!scanRes.isEmpty()) {
                    taskEntity.incrementFoundFiles()
                }
                TaskFiles.update(where = { TaskFiles.id.eq(fileId) }) {
                    it[state] = TaskState.COMPLETED
                }
            }
        } else {
            database.transaction {
                TaskFiles.update(where = { TaskFiles.id.eq(fileId) }) {
                    it[state] = TaskState.FAILED
                }
            }
        }
    }

    /** Remove cached metadata for tasks no longer in SCANNING state. */
    private fun evictStaleTasks() {
        val activeTaskIds = tasks.tasks.value
            .filter { it.state.value == TaskState.SCANNING }
            .map { it.dbTask.id.value }
            .toSet()
        taskMetadataCache.keys.removeAll { it !in activeTaskIds }
    }
}

private data class TaskMetadata(
    val fastScan: Boolean,
    val matchers: Map<IMatcher, Int>,
    val extensions: List<IFileType>
)
