package org.angryscan.app.scan

import MigrationUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.angryscan.common.engine.IMatcher
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.exception.FlywayValidateException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.angryscan.app.common.AppFiles
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.LogMarkers
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.db.DatabaseConnector
import org.angryscan.app.db.models.*
import org.angryscan.app.scan.common.connectors.ConnectorS3
import org.angryscan.app.scan.common.connectors.IConnector
import org.angryscan.app.scan.common.files.FileType
import org.angryscan.app.scan.functions.CertDetectFun
import org.angryscan.app.scan.functions.CodeDetectFun
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.absolutePathString

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

    private var migrationRequired = false

    init {
        if (File(
                AppFiles
                    .MigrationsDirectory
                    .resolve("V2__AddMissingColumns.sql")
                    .absolutePathString()
            ).exists()
        ) {
            File(AppFiles.MigrationsDirectory.absolutePathString()).listFiles()?.forEach {
                it.delete()
            }
            appSettings.firstMigration.value = true
            appSettings.save()
        }

        transaction(database.connection) {
            SchemaUtils.create(
                Tasks,
                TaskFiles,
                TaskFileExtensions,
                TaskMatchers,
                TaskFileScanResults
            )


            val statements = MigrationUtils.statementsRequiredForDatabaseMigration(Tasks, withLogs = false)
            if (statements.isNotEmpty()) {
                logger.info {
                    "Database migration required."
                }
                migrationRequired = true
                if (!File(AppFiles.MigrationsDirectory.absolutePathString()).exists()) {
                    File(AppFiles.MigrationsDirectory.absolutePathString()).mkdir()
                }

                MigrationUtils.generateMigrationScript(
                    Tasks,
                    scriptDirectory = AppFiles.MigrationsDirectory.absolutePathString(),
                    scriptName = "V2__FirstMigration",
                )
            }
        }

        if (migrationRequired) {
            val flyway = Flyway.configure()
                .dataSource(database.dbSettings.url, "", "")
                .defaultSchema("main")
                .schemas("main")
                .locations("filesystem:${AppFiles.MigrationsDirectory}")
                .baselineOnMigrate(appSettings.firstMigration.value)
                .load()
            try {
                flyway.validate()

                val m = runBlocking {
                    database.transaction {
                        flyway.migrate()
                    }
                }

                if (m.success && m.successfulMigrations.isNotEmpty()) {
                    appSettings.firstMigration.value = false
                    appSettings.save()
                    migrationRequired = false
                    logger.info {
                        "Database migration completed."
                    }
                } else {
                    logger.error {
                        "Database migration failed."
                    }
                }
            } catch (_: FlywayValidateException) {

            }
        }

        scanThreads = Array(appSettings.threadCount.value) { ScanThread() }
        CoroutineScope(Dispatchers.IO).launch {
            database.transaction {
                Task.all().forEach { task ->

                    val taskEntity = TaskEntityViewModel(
                        dbTask = task,
                        state = task.taskState,
                        totalFiles = task.filesCount,
                        foundAttributes = (TaskFileScanResults innerJoin TaskFiles innerJoin TaskMatchers)
                            .select(TaskMatchers.matcher)
                            .where { TaskFiles.task.eq(task.id) }
                            .withDistinct()
                            .map { it[TaskMatchers.matcher] }
                            .toSet(),
                        foundFiles = TaskFiles
                            .innerJoin(TaskFileScanResults)
                            .select(TaskFiles.id)
                            .where { TaskFiles.task.eq(task.id) }
                            .withDistinct()
                            .count(),
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
        extensions: List<FileType>,
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
            if(matchers.contains(CodeDetectFun) && !taskExtensions.contains(FileType.CODE)) taskExtensions.add(FileType.CODE)
            if(matchers.contains(CertDetectFun) && !taskExtensions.contains(FileType.CERT)) taskExtensions.add(FileType.CERT)

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