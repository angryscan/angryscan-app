package org.angryscan.app.scan

import androidx.lifecycle.ViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.angryscan.app.common.LogMarkers
import org.angryscan.app.db.DatabaseConnector
import org.angryscan.app.db.models.*
import org.angryscan.app.scan.common.FilesCounter
import org.angryscan.app.scan.common.connectors.FoundedFile
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.common.engine.IMatcher
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

class TaskEntityViewModel(
    val dbTask: Task,
    totalFiles: Long? = null,
    foundAttributes: Map<IMatcher, Int>? = null,
    foundFiles: Long? = null,
    folderSize: String? = null,
    state: TaskState = TaskState.LOADING
) : ViewModel(), KoinComponent {
    private val database: DatabaseConnector by inject()

    private val taskScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    private var _name = MutableStateFlow<String?>(null)
    val name
        get() = _name.asStateFlow()

    private var _state = MutableStateFlow(state)
    val state
        get() = _state.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy
        get() = _busy.asStateFlow()

    private var _id = MutableStateFlow<Int?>(null)
    val id = _id.asStateFlow()

    private var _totalFiles = MutableStateFlow(0L)
    val totalFiles
        get() = _totalFiles.asStateFlow()

    private var _selectedFiles = MutableStateFlow(0L)
    val selectedFiles
        get() = _selectedFiles.asStateFlow()

    private var _scannedFiles = MutableStateFlow(0L)
    val scannedFiles
        get() = _scannedFiles.asStateFlow()

    private var _foundFiles = MutableStateFlow(0L)
    val foundFiles
        get() = _foundFiles.asStateFlow()

    private var _skippedFiles = MutableStateFlow(0L)
    val skippedFiles
        get() = _skippedFiles.asStateFlow()

    private var _foundAttributes = MutableStateFlow(mapOf<IMatcher, Int>())
    val foundAttributes
        get() = _foundAttributes.asStateFlow()

    private var _fastScan = MutableStateFlow(false)
    val fastScan
        get() = _fastScan.asStateFlow()

    private var _path = MutableStateFlow("")
    val path
        get() = _path.asStateFlow()

    private var _startedAt = MutableStateFlow<LocalDateTime?>(null)
    val startedAt
        get() = _startedAt.asStateFlow()

    private var _finishedAt = MutableStateFlow<LocalDateTime?>(null)
    val finishedAt
        get() = _finishedAt.asStateFlow()

    private var _pausedAt = MutableStateFlow<LocalDateTime?>(null)
    val pausedAt
        get() = _pausedAt.asStateFlow()

    private var _deltaSeconds = MutableStateFlow<Long?>(0L)
    val deltaSeconds
        get() = _deltaSeconds.asStateFlow()

    private var _folderSize = MutableStateFlow("")
    val folderSize
        get() = _folderSize.asStateFlow()

    private var _selectedFilesSize = MutableStateFlow(0L)
    val selectedFilesSize
        get() = _selectedFilesSize.asStateFlow()

    private var _foundFilesSize = MutableStateFlow(0L)
    val foundFilesSize
        get() = _foundFilesSize.asStateFlow()

    init {
        if (_state.value == TaskState.LOADING) {
            taskScope.launch {
                database.transaction {
                    _state.value = dbTask.taskState
                    _totalFiles.value = dbTask.filesCount ?: 0L
                    _folderSize.value = dbTask.size ?: ""
                }
            }
        } else {
            if (totalFiles != null)
                _totalFiles.value = totalFiles

            if (foundAttributes != null)
                _foundAttributes.value = foundAttributes

            if (foundFiles != null)
                _foundFiles.value = foundFiles

            if (folderSize != null)
                _folderSize.value = folderSize
        }

        _id.value = dbTask.id.value
        _fastScan.value = dbTask.fastScan
        _path.value = dbTask.path
        _name.value = dbTask.name
        _startedAt.value = dbTask.startedAt
        _pausedAt.value = dbTask.pauseDate
        _deltaSeconds.value = dbTask.delta
        _finishedAt.value = dbTask.finishedAt
        _totalFiles.value = dbTask.filesCount ?: 0L
        if (folderSize != null) _folderSize.value = folderSize
        else _folderSize.value = dbTask.size ?: ""

        taskScope.launch {
            checkProgress()
        }
    }

    fun setFoundStats(foundFiles: Long, foundAttributes: Map<IMatcher, Int>) {
        _foundFiles.value = foundFiles
        _foundAttributes.value = foundAttributes.filter { it.value > 0 }
    }

    fun deleteFoundAttribute(fileId: Int, matcher: IMatcher) {
        taskScope.launch {
            database.transaction {
                val dbMatcher = dbTask.matchers.find { it.matcher::class == matcher::class }!!
                TaskFileScanResults.deleteWhere {
                    TaskFileScanResults.file.eq(fileId) and TaskFileScanResults.matcher.eq(dbMatcher.id)
                }
                _foundAttributes.value =
                    (TaskFileScanResults innerJoin TaskFiles innerJoin TaskMatchers)
                        .select(TaskFileScanResults.count.sum(),TaskMatchers.matcher)
                        .groupBy(TaskMatchers.matcher)
                        .where { TaskFiles.task.eq(dbTask.id) }
                        .associate{ it[TaskMatchers.matcher] to (it[TaskFileScanResults.count.sum()]?: 0) }
                        .filter { it.value > 0 }
            }
        }
    }

    fun addFoundAttribute(matcher: IMatcher, count: Int) {
        val key = _foundAttributes.value.keys.find { it.name == matcher.name }
        if(key == null) {
            _foundAttributes.value += (matcher to count)
        } else {
            _foundAttributes.value = _foundAttributes.value.toMutableMap().apply {
                this[key] = this[key]!! + count
            }
        }
    }

    fun incrementFoundFiles() {
        _foundFiles.value++
    }

    suspend fun checkProgress() {
        while (_state.value == TaskState.LOADING) {
            delay(500)
        }
        if (_state.value != TaskState.PENDING && _state.value != TaskState.SEARCHING) {
            database.transaction {
                if (_selectedFiles.value == 0L) {
                    _selectedFiles.value = TaskFiles
                        .selectAll()
                        .where {
                            TaskFiles.task.eq(dbTask.id)
                        }
                        .count()
                    
                    _selectedFilesSize.value = TaskFiles
                        .select(TaskFiles.size)
                        .where {
                            TaskFiles.task.eq(dbTask.id)
                        }
                        .sumOf { it[TaskFiles.size] }
                }


                _skippedFiles.value = TaskFiles
                    .selectAll()
                    .where {
                        TaskFiles.task.eq(dbTask.id) and TaskFiles.state.eq(TaskState.FAILED)
                    }
                    .count()

                _scannedFiles.value = TaskFiles
                    .selectAll()
                    .where {
                        TaskFiles.task.eq(dbTask.id) and TaskFiles.state.eq(TaskState.COMPLETED)
                    }
                    .count()

                // Calculate found files size (files that have scan results)
                _foundFilesSize.value = TaskFiles
                    .innerJoin(TaskFileScanResults)
                    .select(TaskFiles.size)
                    .where { TaskFiles.task.eq(dbTask.id) }
                    .withDistinct()
                    .sumOf { it[TaskFiles.size] }

                if (_selectedFiles.value == _scannedFiles.value + _skippedFiles.value) {
                    if (_state.value != TaskState.COMPLETED) {
                        setState(TaskState.COMPLETED)
                        logger.info(
                            throwable = null,
                            LogMarkers.UserAction
                        ) { "Scanning task completed. ID: ${_id.value}. Path: \"${_path.value}\"" }
                    }
                }
            }

            _busy.value = false
        }
    }

    @OptIn(ExperimentalTime::class)
    fun setState(state: TaskState) {
        logger.debug { "Task state changed to $state. ID: ${_id.value}. Path: \"${_path.value}\"" }

        taskScope.launch {
            while (_busy.value)
                delay(1000)

            _busy.value = true
            database.transaction {
                dbTask.taskState = state

                when (state) {
                    TaskState.SEARCHING -> {
                        dbTask.startedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        _startedAt.value = dbTask.startedAt

                        if (dbTask.pauseDate != null) {
                            dbTask.delta = (dbTask.delta ?: 0L) +
                                    (Clock.System.now() - dbTask.pauseDate!!.toInstant(TimeZone.currentSystemDefault()))
                                        .toLong(DurationUnit.SECONDS)
                            dbTask.pauseDate = null

                            _pausedAt.value = null
                            _deltaSeconds.value = dbTask.delta
                        }
                    }

                    TaskState.COMPLETED -> {
                        dbTask.finishedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        _finishedAt.value = dbTask.finishedAt
                    }

                    TaskState.STOPPED -> {
                        TaskFiles.update(
                            where = {
                                TaskFiles.task.eq(dbTask.id) and
                                        TaskFiles.state.neq(TaskState.COMPLETED) and
                                        TaskFiles.state.neq(TaskState.FAILED) and
                                        TaskFiles.state.neq(TaskState.SCANNING)
                            }
                        ) {
                            it[TaskFiles.state] = TaskState.STOPPED
                        }
                        if (dbTask.pauseDate == null)
                            dbTask.pauseDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

                        _pausedAt.value = dbTask.pauseDate
                    }

                    TaskState.PENDING -> {
                        if (dbTask.taskState > TaskState.PENDING) {
                            if (dbTask.pauseDate == null)
                                dbTask.pauseDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                            _pausedAt.value = dbTask.pauseDate
                        }
                    }

                    TaskState.SCANNING -> {
                        TaskFiles.update(
                            where = {
                                TaskFiles.task.eq(dbTask.id) and
                                        TaskFiles.state.neq(TaskState.COMPLETED) and
                                        TaskFiles.state.neq(TaskState.FAILED)
                            }
                        ) {
                            it[TaskFiles.state] = TaskState.SEARCHING
                        }

                        if (dbTask.pauseDate != null) {
                            dbTask.delta = (dbTask.delta ?: 0L) +
                                    (Clock.System.now() - dbTask.pauseDate!!.toInstant(TimeZone.currentSystemDefault()))
                                        .toLong(DurationUnit.SECONDS)
                            dbTask.pauseDate = null

                            _pausedAt.value = null
                            _deltaSeconds.value = dbTask.delta
                        }
                    }

                    else -> {}
                }
            }

            _state.value = state

            if (state == TaskState.STOPPED) {
                while (true) {
                    val cnt = database.transaction {
                        TaskFiles
                            .selectAll()
                            .where {
                                TaskFiles.task.eq(dbTask.id) and
                                        TaskFiles.state.neq(TaskState.STOPPED) and
                                        TaskFiles.state.neq(TaskState.COMPLETED) and
                                        TaskFiles.state.neq(TaskState.FAILED)
                            }
                            .count()
                    }
                    if (cnt == 0L)
                        break
                    delay(1000)
                }
            }

            _busy.value = false
        }
    }

    fun stop() {
        if (_state.value != TaskState.COMPLETED)
            setState(TaskState.STOPPED)
    }

    fun resume(taskStarted: () -> Unit = {}) {
        setState(TaskState.SCANNING)
        taskStarted()
    }

    fun rescan(taskStarted: () -> Unit = {}) {
        start(rescan = true) {
            taskStarted()
        }
    }

    fun start(rescan: Boolean = false, taskStarted: () -> Unit = {}) {
        val path = dbTask.path

        taskScope.launch {
            while (_busy.value)
                delay(1000)

            val extensions = database.transaction {
                TaskFileExtensions
                    .select(TaskFileExtensions.extension)
                    .where { TaskFileExtensions.task.eq(dbTask.id) }
                    .map { it[TaskFileExtensions.extension] }
            }
            
            while (_state.value == TaskState.LOADING) {
                delay(500)
            }

            if (dbTask.taskState == TaskState.SEARCHING ||
                (dbTask.taskState != TaskState.PENDING && rescan)
            ) {
                database.transaction {
                    TaskFiles.deleteWhere {
                        task.eq(dbTask.id)
                    }
                }
            }
            if (_state.value == TaskState.PENDING || _state.value == TaskState.SEARCHING || rescan) {
                if (_state.value != TaskState.SEARCHING)
                    setState(TaskState.SEARCHING)
                val filesCounters = path.split(";")
                    .map { dir ->
                        try {
                            scanDirectory(dir, extensions) {
                                taskScope.launch {
                                    database.transaction {
                                        TaskFile.new {
                                            this.task = dbTask
                                            this.path = it.path
                                            this.state = TaskState.PENDING
                                            this.size = it.size
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logger.error { "Failed to scan directory: $dir. ${e.message}" }
                            setState(TaskState.FAILED)
                            return@launch
                        }
                    }
                val directorySize = FilesCounter()
                filesCounters.forEach {
                    directorySize.plus(it)
                }

                database.transaction {
                    dbTask.size = directorySize.filesSize.toString()
                    dbTask.filesCount = directorySize.filesCount
                }
                _totalFiles.value = directorySize.filesCount
                _folderSize.value = directorySize.filesSize.toString()
            }

            setState(TaskState.SCANNING)
            taskStarted()
        }
    }

    private suspend fun scanDirectory(
        dir: String,
        extensions: List<IFileType>,
        fileSelected: (file: FoundedFile) -> Unit
    ): FilesCounter {
        try {
            val res = dbTask.connector.scanDirectory(
                dir = dir,
                extensions = extensions,
                fileSelected = fileSelected
            )
            return res
        } catch (e: Exception) {
            throw e
        }
    }
}