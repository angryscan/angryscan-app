package org.angryscan.app.db.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.angryscan.app.scan.common.connectors.ConnectorFileShare
import org.angryscan.app.scan.common.connectors.IConnector
import org.angryscan.app.serializers.PolymorphicFormatter

object Tasks : IntIdTable() {
    val name = text("name").nullable()
    val path = text("path")
    val taskState = enumeration("task_state", TaskState::class)
    val startedAt = datetime("started_at").nullable()
    val finishedAt = datetime("finished_at").nullable()
    val size = text("size").nullable()
    val filesCount = long("files_count").nullable()
    val fastScan = bool("fast_scan").default(false)
    val pauseDate = datetime("pause_date").nullable()
    val lastFileDate = datetime("last_file_date").nullable()
    val delta = long("delta").nullable()
    val connector = json<IConnector>("matcher", PolymorphicFormatter)
        .default(ConnectorFileShare())
}

class Task(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Task>(Tasks)

    var name by Tasks.name
    var path by Tasks.path
    var taskState by Tasks.taskState
    var startedAt by Tasks.startedAt
    var finishedAt by Tasks.finishedAt
    var size by Tasks.size
    var filesCount by Tasks.filesCount
    var fastScan by Tasks.fastScan
    var pauseDate by Tasks.pauseDate
    var lastFileDate by Tasks.lastFileDate
    var delta by Tasks.delta
    var connector by Tasks.connector

    val files by TaskFile referrersOn TaskFiles.task
    val extensions by TaskFileExtension referrersOn TaskFileExtensions.task
    val matchers by TaskMatcher referrersOn TaskMatchers.task
}