package org.angryscan.app.db.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object TaskFiles : IntIdTable() {
    val task = reference("task", Tasks)
    val path = text("path")
    val state = enumeration("state", TaskState::class)
    val size = long("size")
}

class TaskFile(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TaskFile>(TaskFiles)

    var task by Task referencedOn TaskFiles.task
    var path by TaskFiles.path
    var state by TaskFiles.state
    var size by TaskFiles.size
}