package org.angryscan.app.scan

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.angryscan.common.engine.IScanEngine
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.db.DatabaseConnector
import org.angryscan.app.db.models.TaskMatchers
import org.angryscan.app.db.models.TaskFileScanResults
import org.angryscan.app.db.models.TaskFiles
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.scan.common.files.FileType
import org.angryscan.app.scan.engine.fallback
import org.angryscan.app.scan.engine.getEngine
import org.angryscan.app.scan.engine.inappropriateMatchers
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

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

    suspend fun stop() {
        logger.debug { "Stop requested for scan thread [$scanThreadScope]." }
        stopRequested.set(true)

        scanThreadScope.launch {
            while (_started.get())
                delay(1000)
            logger.debug { "Scan thread [$scanThreadScope] stopped by request." }
        }.join()
    }

    fun start() {
        logger.debug { "Starting scan thread [$scanThreadScope]." }
        _started.set(true)
        scanThreadScope.launch {
            val scanSettings = inject<ScanSettings>()
            while (_started.get() && !stopRequested.get()) {
                yield()
                val tasksToScan = tasks.tasks.value.filter { it.state.value == TaskState.SCANNING }
                if (tasksToScan.isEmpty()) {
                    retryCount++
                    if (retryCount > 3) {
                        _started.set(false)
                        retryCount = 0
                        logger.debug { "Nothing to scan. Scan thread [$scanThreadScope] stopped." }
                    }
                    delay(1000)
                    continue
                }

                retryCount = 0

                val taskEntity = tasks.tasks.value.filter { it.state.value == TaskState.SCANNING }.random()

                val dbFile = database.transaction {
                    val resultRow = TaskFiles.selectAll()
                        .where {
                            TaskFiles.task.eq(taskEntity.dbTask.id) and
                                    TaskFiles.state.eq(TaskState.SEARCHING)
                        }
                        .limit(1)
                        .firstOrNull()
                    if (resultRow != null) {
                        TaskFiles.update(
                            where = {
                                TaskFiles.id.eq(resultRow[TaskFiles.id])
                            }
                        ) {
                            it[state] = TaskState.SCANNING
                        }
                    }

                    resultRow
                }

                if (dbFile == null) {
                    scanningFileId.set(-1)

                    retryCount++
                    if (retryCount > 3) {
                        _started.set(false)
                        retryCount = 0
                    }
                    delay(1000)
                    continue
                }

                val fastScan = database.transaction { taskEntity.dbTask.fastScan }

                val fileId = dbFile[TaskFiles.id].value
                val filePath = dbFile[TaskFiles.path]
                val fileObject = taskEntity.dbTask.connector.getFile(filePath)

                val matchers = database.transaction {
                    taskEntity.dbTask.lastFileDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

                    TaskMatchers
                        .select(TaskMatchers.matcher, TaskMatchers.id)
                        .where { TaskMatchers.task.eq(taskEntity.dbTask.id) }
                        .associate { it[TaskMatchers.matcher] to it[TaskMatchers.id].value }
                }
                val engines: MutableList<IScanEngine> = mutableListOf()
                engines.add(
                    scanSettings.value.engine.value.getEngine(matchers.map { it.key })
                )
                val iMatchers = engines[0].inappropriateMatchers(matchers.map { it.key }).toMutableList()
                do {
                    val fbe = engines.last().fallback().getEngine(iMatchers)
                    iMatchers.removeAll(fbe.matchers)
                } while (iMatchers.isNotEmpty() || fbe::class == engines[0])


                scanningFileId.set(fileId)

                val timer = measureTimeMillis {

                    val scanRes = FileType
                        .getFileType(fileObject)
                        ?.scanFile(
                            file = fileObject,
                            context = currentCoroutineContext(),
                            engines = engines,
                            fastScan = fastScan
                        )

                    engines.forEach { eng ->
                        if(eng is AutoCloseable) {
                            eng.close()
                        }
                    }


                    if (scanRes != null && !scanRes.skipped()) {
                        database.transaction {
                            scanRes.getDocumentFields().forEach { field ->
                                TaskFileScanResults.insert {
                                    it[file] = fileId
                                    it[matcher] = matchers[field.key] ?: 0
                                    it[count] = field.value
                                }
                                taskEntity.addFoundAttribute(field.key)
                            }
                            if (!scanRes.isEmpty()) {
                                taskEntity.incrementFoundFiles()
                            }
                            TaskFiles.update(
                                where = {
                                    TaskFiles.id.eq(fileId)
                                }
                            ) {
                                it[state] = TaskState.COMPLETED
                            }
                        }
                    } else {
                        database.transaction {
                            TaskFiles.update(
                                where = {
                                    TaskFiles.id.eq(fileId)
                                }
                            ) {
                                it[state] = TaskState.FAILED
                            }
                        }
                    }
                }
                logger.debug { "Scanned file with extension ${fileObject.extension} and size ${fileObject.length()} in $timer ms" }

                taskEntity.checkProgress()
                yield()
            }

            _started.set(false)
            stopRequested.set(false)
        }
    }
}