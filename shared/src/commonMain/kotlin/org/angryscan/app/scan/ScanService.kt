package org.angryscan.app.scan

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.LogMarkers
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.db.DatabaseConnector
import org.angryscan.app.db.models.*
import org.angryscan.app.scan.common.connectors.ConnectorPostgres
import org.angryscan.app.scan.common.connectors.ConnectorS3
import org.angryscan.app.scan.common.connectors.IConnector
import org.angryscan.app.scan.common.files.types.CertFileType
import org.angryscan.app.scan.common.files.types.CodeFileType
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.app.scan.functions.CertDetectFun
import org.angryscan.app.scan.functions.CodeDetectFun
import org.angryscan.common.engine.IMatcher
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}
private const val StartupStatsTaskLimit = 24

data class TaskReplaySettings(
    val path: String,
    val fastScan: Boolean,
    val connector: IConnector,
    val extensions: List<IFileType>,
    val matchers: List<IMatcher>,
)

@OptIn(ExperimentalDatabaseMigrationApi::class)
class ScanService : KoinComponent {
    private val database: DatabaseConnector by inject()

    private val appSettings: AppSettings by inject()
    private val scanSettings: ScanSettings by inject()

    val tasks: TasksViewModel by inject()

    private var scanThreads: Array<ScanThread>

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    val changingThreadsCount = AtomicBoolean(false)
    private val historyBatchPauseRequests = AtomicInteger(0)

    init {
        DatabaseMigration.migrate()
        scanThreads = Array(appSettings.threadCount.value) { ScanThread() }

        CoroutineScope(Dispatchers.IO).launch {
            val allTasks = database.transaction {
                val tasksList = Task.all().toList()
                tasksList.forEach { task ->
                    if (task.taskState == TaskState.SCANNING) {
                        task.pauseDate = task.lastFileDate
                        TaskFiles.update(
                            where = {
                                TaskFiles.task.eq(task.id) and
                                    TaskFiles.state.neq(TaskState.STOPPED) and
                                    TaskFiles.state.neq(TaskState.COMPLETED) and
                                    TaskFiles.state.neq(TaskState.FAILED)
                            }
                        ) {
                            it[state] = TaskState.STOPPED
                        }
                        logger.info(throwable = null, LogMarkers.UserAction) {
                            "Stopped task after restart (${task.id.value}) ${task.path}"
                        }
                    }
                    if (task.taskState == TaskState.SEARCHING) {
                        task.pauseDate = task.startedAt
                        TaskFiles.deleteWhere {
                            TaskFiles.task.eq(task.id)
                        }
                        logger.info(throwable = null, LogMarkers.UserAction) {
                            "Reset task after restart (${task.id.value}) ${task.path}"
                        }
                    }
                }
                tasksList
            }

            val entities = allTasks.map { task ->
                val state = when (task.taskState) {
                    TaskState.SCANNING -> TaskState.STOPPED
                    TaskState.SEARCHING -> TaskState.PENDING
                    else -> task.taskState
                }
                TaskEntityViewModel(
                    dbTask = task,
                    state = state,
                    totalFiles = task.filesCount,
                    foundAttributes = null,
                    foundFiles = null,
                    folderSize = task.size,
                    bootstrapProgress = false
                )
            }
            tasks.setAll(entities)

            val prioritizedTaskIds = allTasks
                .sortedByDescending { it.finishedAt }
                .sortedByDescending { it.pauseDate }
                .sortedByDescending { it.startedAt }
                .map { it.id.value }
                .take(StartupStatsTaskLimit)
                .toList()
            val prioritizedTaskIdsSet = prioritizedTaskIds.toSet()
            val entitiesByTaskId = entities
                .mapNotNull { entity -> entity.id.value?.let { it to entity } }
                .toMap()

            // Let interactive screens (e.g. Database source controls) win DB queue right after startup.
            delay(250)

            loadStatsForTaskIds(
                taskIds = prioritizedTaskIds,
                entitiesByTaskId = entitiesByTaskId,
                batchSize = 8,
                interBatchDelayMs = 0
            )

            val remainingTaskIds = allTasks
                .sortedByDescending { it.finishedAt }
                .sortedByDescending { it.pauseDate }
                .sortedByDescending { it.startedAt }
                .map { it.id.value }
                .filter { it !in prioritizedTaskIdsSet }

            if (remainingTaskIds.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    // Defer non-priority historical stats to keep the UI responsive.
                    delay(1200)
                    loadStatsForTaskIds(
                        taskIds = remainingTaskIds,
                        entitiesByTaskId = entitiesByTaskId,
                        batchSize = 6,
                        interBatchDelayMs = 120
                    )
                }
            }
        }
    }

    private suspend fun loadStatsForTaskIds(
        taskIds: List<Int>,
        entitiesByTaskId: Map<Int, TaskEntityViewModel>,
        batchSize: Int,
        interBatchDelayMs: Long
    ) {
        if (taskIds.isEmpty()) return

        taskIds.chunked(batchSize).forEach { chunkIds ->
            if (chunkIds.isEmpty()) return@forEach
            waitWhileHistoryBatchesPaused()

            val (foundFilesStatsByTaskId, foundAttributesByTaskId) = database.transaction {
                val foundFilesStats = (TaskFiles innerJoin TaskFileScanResults)
                    .select(TaskFiles.task, TaskFiles.id, TaskFiles.size)
                    .where { TaskFiles.task inList chunkIds }
                    .withDistinct()
                    .groupBy { row -> row[TaskFiles.task].value }
                    .mapValues { (_, rows) ->
                        val foundFilesCount = rows.size.toLong()
                        val foundFilesSize = rows.sumOf { row -> row[TaskFiles.size] }
                        foundFilesCount to foundFilesSize
                    }

                val foundAttributesStats = (TaskFileScanResults innerJoin TaskFiles innerJoin TaskMatchers)
                    .select(TaskFiles.task, TaskMatchers.matcher, TaskFileScanResults.count.sum())
                    .where { TaskFiles.task inList chunkIds }
                    .groupBy(TaskFiles.task, TaskMatchers.matcher)
                    .map { row ->
                        val taskId = row[TaskFiles.task].value
                        val matcher = row[TaskMatchers.matcher]
                        val sum = row[TaskFileScanResults.count.sum()] ?: 0
                        Triple(taskId, matcher, sum)
                    }
                    .groupBy { it.first }
                    .mapValues { (_, rows) ->
                        rows
                            .asSequence()
                            .map { (_, matcher, sum) -> matcher to sum }
                            .filter { it.second > 0 }
                            .associate { it.first to it.second }
                    }

                foundFilesStats to foundAttributesStats
            }

            chunkIds.forEach { taskId ->
                val entity = entitiesByTaskId[taskId] ?: return@forEach
                val (foundFilesCount, foundFilesSize) = foundFilesStatsByTaskId[taskId] ?: (0L to 0L)
                val foundAttributes = foundAttributesByTaskId[taskId].orEmpty()
                entity.setFoundStats(
                    foundFiles = foundFilesCount,
                    foundAttributes = foundAttributes,
                    foundFilesSize = foundFilesSize
                )
            }

            yield()
            if (interBatchDelayMs > 0) delay(interBatchDelayMs)
        }
    }

    private suspend fun waitWhileHistoryBatchesPaused() {
        while (historyBatchPauseRequests.get() > 0) {
            delay(60)
        }
    }

    suspend fun <T> withHistoryBatchesPaused(block: suspend () -> T): T {
        historyBatchPauseRequests.incrementAndGet()
        return try {
            block()
        } finally {
            val left = historyBatchPauseRequests.decrementAndGet()
            if (left < 0) historyBatchPauseRequests.set(0)
        }
    }

    fun start() {
        logger.info(throwable = null, LogMarkers.UserAction) {
            "Starting scan threads"
        }
        coroutineScope.launch {
            while (changingThreadsCount.get())
                delay(1000)

            scanThreads.forEach {
                if (!it.started)
                    it.start()
            }
        }

    }

    suspend fun stop() {
        logger.info(throwable = null, LogMarkers.UserAction) {
            "Stopping scan threads"
        }
        coroutineScope {
            scanThreads.map {
                async {
                    if (it.started)
                        it.stop()
                }
            }.awaitAll()
        }
    }

    fun setThreadsCount() {
        val scanStarted = scanThreads.any { it.started }
        logger.info(throwable = null, LogMarkers.UserAction) {
            "Setting scan threads to ${appSettings.threadCount.value}"
        }
        coroutineScope.launch {
            changingThreadsCount.set(true)
            if (scanStarted)
                stop()

            scanThreads = Array(appSettings.threadCount.value) { ScanThread() }
            if (scanStarted)
                start()
            changingThreadsCount.set(false)
        }
    }

    suspend fun createTask(
        name: String? = null,
        path: String,
        extensions: List<IFileType>,
        matchers: List<IMatcher>,
        fastScan: Boolean? = null,
        connector: IConnector
    ): TaskEntityViewModel {
        return withHistoryBatchesPaused {
            database.transaction {
                val task = Task.new {
                    this.name = name
                    this.path = path
                    this.taskState = TaskState.PENDING
                    this.fastScan = fastScan ?: scanSettings.fastScan.value
                    this.connector = connector
                }
                logger.info(throwable = null, LogMarkers.UserAction) {
                    "Creating task. " +
                            "ID: ${task.id.value}. " +
                            "Path: \"$path\". " +
                            "Extensions: ${
                                extensions.joinToString { it.name }
                            }. " +
                            "Detect functions: ${
                                matchers.joinToString { it.name }
                            }. " +
                            "Fast scan: ${fastScan ?: scanSettings.fastScan.value}. " +
                            "Threads: ${appSettings.threadCount.value}. " +
                            "Connector: $connector ." +
                            if (connector is ConnectorS3) {
                                "Endpoind: ${connector.endpointStr}. " +
                                        "Bucket: ${connector.bucketStr}. " +
                                        "Region: ${connector.regionStr}. "
                            } else if (connector is ConnectorPostgres) {
                                "Host: ${connector.host}. " +
                                        "Port: ${connector.port}. " +
                                        "Database: ${connector.database}. "
                            } else ""
                }

                matchers.forEach { m ->
                    TaskMatcher.new {
                        this.task = task
                        matcher = m
                    }
                }

                val taskExtensions = extensions.toMutableList()
                if (matchers.contains(CodeDetectFun)) {
                    CodeFileType.entries.forEach {
                        if (!taskExtensions.contains(it))
                            taskExtensions.add(it)
                    }
                }

                if (matchers.contains(CertDetectFun)) {
                    CertFileType.entries.forEach {
                        if (!taskExtensions.contains(it))
                            taskExtensions.add(it)
                    }
                }

                taskExtensions.forEach { ext ->
                    TaskFileExtension.new {
                        this.task = task
                        this.extension = ext
                    }
                }

                val taskEntity = TaskEntityViewModel(task)
                tasks.add(taskEntity)
                taskEntity
            }
        }
    }

    suspend fun deleteTask(task: TaskEntityViewModel) {
        task.stop()
        logger.info(throwable = null, LogMarkers.UserAction) {
            "Delete task. ID: ${task.id.value}. Path: \"${task.path.value}\""
        }
        database.transaction {
            TaskFileExtensions.deleteWhere {
                TaskFileExtensions.task.eq(task.dbTask.id)
            }
            TaskFiles.deleteWhere {
                TaskFiles.task.eq(task.dbTask.id)
            }
            task.dbTask.delete()
        }
        tasks.delete(task)
    }

    fun startTask(task: TaskEntityViewModel) {
        logger.info(throwable = null, LogMarkers.UserAction) {
            "Starting task. ID: ${task.id.value}. Path: \"${task.path.value}\""
        }
        task.start {
            this.start()
        }
    }

    fun stopTask(task: TaskEntityViewModel) {
        logger.info(throwable = null, LogMarkers.UserAction) {
            "Stopping task. ID: ${task.id.value}. Path: \"${task.path.value}\""
        }
        task.stop()
    }

    fun resumeTask(task: TaskEntityViewModel) {
        logger.info(throwable = null, LogMarkers.UserAction) {
            "Resume task. ID: ${task.id.value}. Path: \"${task.path.value}\""
        }
        task.resume {
            this.start()
        }
    }

    fun rescanTask(task: TaskEntityViewModel) {
        logger.info(throwable = null, LogMarkers.UserAction) {
            "Restart task. ID: ${task.id.value}. Path: \"${task.path.value}\""
        }
        task.rescan {
            this.start()
        }
    }

    suspend fun snapshotTaskReplaySettings(task: TaskEntityViewModel): TaskReplaySettings = database.transaction {
        TaskReplaySettings(
            path = task.dbTask.path,
            fastScan = task.dbTask.fastScan,
            connector = task.dbTask.connector,
            extensions = task.dbTask.extensions.map { it.extension },
            matchers = task.dbTask.matchers.map { it.matcher }
        )
    }

    suspend fun startNewScanFromTask(task: TaskEntityViewModel): TaskEntityViewModel {
        val replay = snapshotTaskReplaySettings(task)
        val newTask = createTask(
            name = task.name.value,
            path = replay.path,
            extensions = replay.extensions,
            matchers = replay.matchers,
            fastScan = replay.fastScan,
            connector = replay.connector
        )
        startTask(newTask)
        return newTask
    }
}