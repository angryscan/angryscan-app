package org.angryscan.app.scan

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.angryscan.app.db.DatabaseConnector
import org.angryscan.app.db.models.*
import org.angryscan.app.scan.common.FileSize
import org.angryscan.app.scan.common.connectors.IDatabaseConnector
import org.angryscan.app.scan.functions.CertDetectFun
import org.angryscan.app.scan.functions.CodeDetectFun
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.matchers.CardNumber
import org.angryscan.common.matchers.FullName
import org.jetbrains.exposed.sql.and
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class TaskFileResult(
    val id: Int,
    val path: String,
    val size: FileSize,
    val foundAttributes: Map<IMatcher, Int>,
    val count: Int,
    val score: Long,
    /** For DB scan: column where matches were found. Null for file scan. */
    val columnName: String? = null
)

class TaskFilesViewModel(val task: Task) : KoinComponent, ViewModel() {
    private val database: DatabaseConnector by inject()

    private val taskScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private val _taskFiles = MutableStateFlow<List<TaskFileResult>>(listOf())
    val taskFiles
        get() = _taskFiles.asStateFlow()

    private val _scoreSum = MutableStateFlow(0L)
    val scoreSum
        get() = _scoreSum.asStateFlow()

    private val _updated = MutableStateFlow(false)
    val updated
        get() = _updated.asStateFlow()

    init {
        taskScope.launch {
            update()
            _updated.value = true
        }
    }

    suspend fun update() {
        _updated.value = false
        database.transaction {
            val isDbTask = task.connector is IDatabaseConnector
            if (isDbTask) {
                // DB scan: group by (file, column_name) so each row is table+column with its matcher counts
                val rows = (TaskFiles innerJoin TaskFileScanResults innerJoin TaskMatchers)
                    .select(
                        TaskFiles.id,
                        TaskFiles.path,
                        TaskFiles.size,
                        TaskFileScanResults.columnName,
                        TaskMatchers.matcher,
                        TaskFileScanResults.count
                    )
                    .where {
                        TaskFiles.task.eq(task.id) and TaskFiles.state.eq(TaskState.COMPLETED)
                    }
                    .map { r ->
                        Triple(
                            Triple(r[TaskFiles.id].value, r[TaskFiles.path], FileSize(r[TaskFiles.size])),
                            r[TaskFileScanResults.columnName],
                            r[TaskMatchers.matcher] to r[TaskFileScanResults.count]
                        )
                    }
                val grouped = rows.groupBy { (fileKey, columnName, _) -> fileKey to columnName }
                _taskFiles.value = grouped.map { (key, groupRows) ->
                    val (fileId, path, size) = key.first
                    val columnName = key.second
                    val detectRows = groupRows.map { it.third }
                    val containsFIO = detectRows.map { it.first }.contains(FullName)
                    TaskFileResult(
                        id = fileId,
                        path = path,
                        size = size,
                        foundAttributes = detectRows.groupingBy { it.first }.fold(0) { acc, (_, c) -> acc + c },
                        count = detectRows.sumOf { it.second },
                        score = detectRows.sumOf { row ->
                            (if (containsFIO) 20 else detectRows.size - 1) +
                                    (when (row.first) {
                                        is FullName -> 5f
                                        is CardNumber -> 30f
                                        is CodeDetectFun -> 0.01f
                                        is CertDetectFun -> 100f
                                        else -> 1f
                                    } * row.second).toLong()
                        },
                        columnName = columnName
                    )
                }
            } else {
                val fileRows = TaskFiles
                    .innerJoin(TaskFileScanResults)
                    .select(
                        TaskFiles.path,
                        TaskFiles.size,
                        TaskFiles.id
                    )
                    .where {
                        TaskFiles.task.eq(task.id) and TaskFiles.state.eq(TaskState.COMPLETED)
                    }
                    .withDistinct()

                _taskFiles.value = fileRows.map { fileRow ->
                    val detectRows = TaskFileScanResults
                        .innerJoin(TaskMatchers)
                        .select(TaskMatchers.matcher, TaskFileScanResults.count)
                        .where { TaskFileScanResults.file.eq(fileRow[TaskFiles.id]) }
                        .map { it[TaskMatchers.matcher] to it[TaskFileScanResults.count] }

                    val containsFIO = detectRows.map { it.first }.contains(FullName)

                    TaskFileResult(
                        id = fileRow[TaskFiles.id].value,
                        path = fileRow[TaskFiles.path],
                        size = FileSize(fileRow[TaskFiles.size]),
                        foundAttributes = detectRows.toMap(),
                        count = detectRows.sumOf { it.second },
                        score = detectRows.sumOf { row ->
                            (if (containsFIO) 20 else detectRows.size - 1) +
                                    (when (row.first) {
                                        is FullName -> 5f
                                        is CardNumber -> 30f
                                        is CodeDetectFun -> 0.01f
                                        is CertDetectFun -> 100f
                                        else -> 1f
                                    } * row.second).toLong()
                        }
                    )
                }
            }
            _scoreSum.value = _taskFiles.value.sumOf { it.score }
        }
    }
}