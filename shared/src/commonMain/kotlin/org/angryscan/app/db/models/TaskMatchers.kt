package org.angryscan.app.db.models

import org.angryscan.common.engine.IMatcher
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.json
import org.angryscan.app.serializers.PolymorphicFormatter

object TaskMatchers : IntIdTable() {
    val task = reference("task", Tasks)
    val matcher = json<IMatcher>("matcher", PolymorphicFormatter)
}

class TaskMatcher(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TaskMatcher>(TaskMatchers)

    var task by Task referencedOn TaskMatchers.task
    var matcher by TaskMatchers.matcher
}