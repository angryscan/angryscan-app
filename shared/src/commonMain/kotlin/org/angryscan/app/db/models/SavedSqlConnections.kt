package org.angryscan.app.db.models

import org.angryscan.app.common.DatabaseType
import org.jetbrains.exposed.sql.Table

object SavedSqlConnections : Table("saved_sql_connections") {
    val connectionKey = text("connection_key").uniqueIndex()
    val name = text("name")
    val databaseType = enumeration("database_type", DatabaseType::class)
    val host = text("host")
    val port = text("port")
    val database = text("database")
    val schema = text("schema")
    val user = text("user")
    /** MongoDB SCRAM auth database (optional); empty for other DB types. */
    val authDatabase = text("auth_database").default("")
}

