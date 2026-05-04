package org.angryscan.app.common

data class SavedSqlConnection(
    val connectionKey: String,
    val name: String,
    val databaseType: DatabaseType,
    val host: String,
    val port: String,
    val database: String,
    val schema: String,
    val user: String,
)

