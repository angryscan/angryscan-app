package org.angryscan.app.scan

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.LogMarkers
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.db.DatabaseConnector
import org.angryscan.app.db.models.*
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
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalDatabaseMigrationApi::class)
class ScanService : KoinComponent {
    private val database: DatabaseConnector by inject()

    private val appSettings: AppSettings by inject()
    private val scanSettings: ScanSettings by inject()

    val tasks: TasksViewModel by inject()

    private var scanThreads: Array<ScanThread>

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    val changingThreadsCount = AtomicBoolean(false)

    init {
        DatabaseMigration.migrate()
        scanThreads = Array(appSettings.threadCount.value) { ScanThread() }
        
        CoroutineScope(Dispatchers.IO).launch {
            // Load all tasks in one transaction first
            val allTasks = database.transaction {
                Task.all().toList()
            }
            
            // Process each task in separate transactions to avoid blocking
            allTasks.forEach { task ->
                database.transaction {
                    val foundAttributes = (TaskFileScanResults innerJoin TaskFiles innerJoin TaskMatchers)
                        .select(TaskFileScanResults.count.sum(),TaskMatchers.matcher)
                        .groupBy(TaskMatchers.matcher)
                        .where { TaskFiles.task.eq(task.id) }
                        .associate{ it[TaskMatchers.matcher] to (it[TaskFileScanResults.count.sum()]?: 0) }
                        .filter { it.value > 0 }
                    
                    val foundFiles = TaskFiles
                        .innerJoin(TaskFileScanResults)
                        .select(TaskFiles.id)
                        .where { TaskFiles.task.eq(task.id) }
                        .withDistinct()
                        .count()
                    
                    val taskEntity = TaskEntityViewModel(
                        dbTask = task,
                        state = task.taskState,
                        totalFiles = task.filesCount,
                        foundAttributes = foundAttributes,
                        foundFiles = foundFiles,
                        folderSize = task.size
                    )
                    
                    if (task.taskState == TaskState.SCANNING) {
                        task.pauseDate = task.lastFileDate

                        taskEntity.setState(TaskState.STOPPED)
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
                            "Stopped task after restart (${taskEntity.id.value}) ${taskEntity.path.value}"
                        }
                    }

                    if (task.taskState == TaskState.SEARCHING) {
                        task.pauseDate = task.startedAt

                        taskEntity.setState(TaskState.PENDING)
                        TaskFiles.deleteWhere {
                            TaskFiles.task.eq(task.id)
                        }

                        logger.info(throwable = null, LogMarkers.UserAction) {
                            "Reset task after restart (${taskEntity.id.value}) ${taskEntity.path.value}"
                        }
                    }

                    tasks.add(taskEntity)
                }
            }
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
        return database.transaction {
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

    @OptIn(ExperimentalTime::class)
    suspend fun completeAIModelTask(task: TaskEntityViewModel, resultJson: String) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val root = runCatching {
            Json.parseToJsonElement(resultJson) as? JsonObject
        }.getOrNull()
        val filesScanned = root?.get("files_scanned")?.jsonPrimitive?.content?.toLongOrNull() ?: 1L
        val duration = root?.get("duration")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
        val durationCeilSeconds = kotlin.math.ceil(duration).toLong().coerceAtLeast(0L)
        val failedChecks = root?.get("failed_checks")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        val issuesArray = root?.get("issues") as? JsonArray
        val foundFilesWithIssues = if (issuesArray != null) {
            issuesArray
                .mapNotNull { (it as? JsonObject)?.get("location")?.jsonPrimitive?.content }
                .map { loc -> loc.substringBefore(":").trim().ifEmpty { loc } }
                .toSet().size.toLong()
        } else 0L
        val startedAt = (Clock.System.now() - durationCeilSeconds.toDuration(DurationUnit.SECONDS))
            .toLocalDateTime(TimeZone.currentSystemDefault())
        database.transaction {
            task.dbTask.resultJson = resultJson
            task.dbTask.startedAt = startedAt
            task.dbTask.finishedAt = now
            task.dbTask.filesCount = filesScanned
        }
        task.setAIModelResultData(resultJson, startedAt, now, filesScanned, foundFilesWithIssues, failedChecks)
        kotlinx.coroutines.delay(500)
        task.setState(TaskState.COMPLETED)
        logger.info(throwable = null, LogMarkers.UserAction) {
            "AI Model scan completed. ID: ${task.id.value}. Path: \"${task.path.value}\""
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
}