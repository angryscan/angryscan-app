package org.angryscan.app.db.models

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

val format = Json { prettyPrint = false }

object TaskFileScanResults: IntIdTable() {
    val file = reference("file", TaskFiles)
    val matcher = reference("matcher", TaskMatchers)
    val count = integer("count")
}

class TaskFileScanResult(id: EntityID<Int>) : IntEntity(id) {
    companion object: IntEntityClass<TaskFileScanResult>(TaskFileScanResults)
    var file by TaskFile referencedOn TaskFileScanResults.file
    var matcher by TaskMatcher referencedOn TaskFileScanResults.matcher
    var count by TaskFileScanResults.count
}