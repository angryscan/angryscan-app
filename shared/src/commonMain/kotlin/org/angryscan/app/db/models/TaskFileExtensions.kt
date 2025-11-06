package org.angryscan.app.db.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.angryscan.app.scan.common.files.FileType

object TaskFileExtensions : IntIdTable() {
    val task = reference("task", Tasks)
    val extension = enumeration("extension", FileType::class)
}

class TaskFileExtension(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<TaskFileExtension>(TaskFileExtensions)

    var task by Task referencedOn TaskFileExtensions.task
    var extension by TaskFileExtensions.extension
}