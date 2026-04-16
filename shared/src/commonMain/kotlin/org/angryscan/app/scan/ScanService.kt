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
                    folderSize = task.size
                )
            }
            tasks.setAll(entities)

            val (foundFilesMap, foundAttributesMap) = database.transaction {
                val foundFilesByTask = (TaskFiles innerJoin TaskFileScanResults)
                    .select(TaskFiles.task, TaskFiles.id)
                    .withDistinct()
                    .map { row -> row[TaskFiles.task].value to row[TaskFiles.id].value }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { it.value.distinct().size }

                val foundAttrsByTask = (TaskFileScanResults innerJoin TaskFiles innerJoin TaskMatchers)
                    .select(TaskFiles.task, TaskMatchers.matcher, TaskFileScanResults.count.sum())
                    .groupBy(TaskFiles.task, TaskMatchers.matcher)
                    .map { row ->
                        val taskId = row[TaskFiles.task].value
                        val matcher = row[TaskMatchers.matcher]
                        val sum = row[TaskFileScanResults.count.sum()] ?: 0
                        Triple(taskId, matcher, sum)
                    }
                    .filter { it.third > 0 }
                    .groupBy({ it.first }, { it.second to it.third })
                    .mapValues { entries -> entries.value.associate { it.first to it.second } }

                Pair(foundFilesByTask, foundAttrsByTask)
            }

            entities.forEach { entity ->
                val taskId = entity.id.value ?: return@forEach
                entity.setFoundStats(
                    foundFiles = foundFilesMap.getOrDefault(taskId, 0).toLong(),
                    foundAttributes = foundAttributesMap.getOrDefault(taskId, emptyMap())
                )
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
                        } else if (connector is ConnectorPostgres) {
                            "Host: ${connector.host}. " +
                                    "Port: ${connector.port}. " +
                                    "Database: ${connector.database}. " +
                                    "Row limit: ${connector.rowLimit}. "
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