package org.angryscan.app.common

import org.angryscan.app.db.DatabaseConnector
import org.angryscan.app.db.models.SavedSqlConnections
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class SavedSqlConnectionsRepository(
    private val databaseConnector: DatabaseConnector,
    private val secretStore: SqlConnectionSecretStore,
) {
    fun connectionKey(
        databaseType: DatabaseType,
        host: String,
        port: String,
        schema: String,
        user: String,
        authDatabase: String = "",
    ): String = buildString {
        append(databaseType.name)
        append("|")
        append(host.trim().lowercase())
        append("|")
        append(port.trim())
        append("|")
        append(schema.trim().lowercase())
        append("|")
        append(user.trim().lowercase())
        val adb = authDatabase.trim().lowercase()
        if (adb.isNotEmpty()) {
            append("|adb:").append(adb)
        }
    }

    suspend fun list(databaseType: DatabaseType? = null): List<SavedSqlConnection> =
        databaseConnector.transaction {
            SavedSqlConnections
                .selectAll()
                .let { query ->
                    if (databaseType == null) query
                    else query.where { SavedSqlConnections.databaseType eq databaseType }
                }
                .map {
                    SavedSqlConnection(
                        connectionKey = it[SavedSqlConnections.connectionKey],
                        name = it[SavedSqlConnections.name],
                        databaseType = it[SavedSqlConnections.databaseType],
                        host = it[SavedSqlConnections.host],
                        port = it[SavedSqlConnections.port],
                        database = it[SavedSqlConnections.database],
                        schema = it[SavedSqlConnections.schema],
                        user = it[SavedSqlConnections.user],
                        authDatabase = it[SavedSqlConnections.authDatabase],
                    )
                }
        }

    suspend fun upsert(
        name: String,
        databaseType: DatabaseType,
        host: String,
        port: String,
        database: String,
        schema: String,
        user: String,
        password: String,
        authDatabase: String = "",
    ): String {
        val normalizedName = name.trim()
        val normalizedHost = host.trim()
        val normalizedPort = port.trim()
        val normalizedDatabase = database.trim()
        val normalizedSchema = schema.trim()
        val normalizedUser = user.trim()
        val normalizedAuthDb = authDatabase.trim()

        val key = connectionKey(
            databaseType = databaseType,
            host = normalizedHost,
            port = normalizedPort,
            schema = normalizedSchema,
            user = normalizedUser,
            authDatabase = normalizedAuthDb
        )

        databaseConnector.transaction {
            val updated = SavedSqlConnections.update({ SavedSqlConnections.connectionKey eq key }) {
                it[SavedSqlConnections.name] = normalizedName
                it[SavedSqlConnections.databaseType] = databaseType
                it[SavedSqlConnections.host] = normalizedHost
                it[SavedSqlConnections.port] = normalizedPort
                it[SavedSqlConnections.database] = normalizedDatabase
                it[SavedSqlConnections.schema] = normalizedSchema
                it[SavedSqlConnections.user] = normalizedUser
                it[SavedSqlConnections.authDatabase] = normalizedAuthDb
            }
            if (updated == 0) {
                SavedSqlConnections.insert {
                    it[connectionKey] = key
                    it[SavedSqlConnections.name] = normalizedName
                    it[SavedSqlConnections.databaseType] = databaseType
                    it[SavedSqlConnections.host] = normalizedHost
                    it[SavedSqlConnections.port] = normalizedPort
                    it[SavedSqlConnections.database] = normalizedDatabase
                    it[SavedSqlConnections.schema] = normalizedSchema
                    it[SavedSqlConnections.user] = normalizedUser
                    it[SavedSqlConnections.authDatabase] = normalizedAuthDb
                }
            }
        }

        if (password.isNotEmpty()) {
            secretStore.setPassword(key, password)
        }

        return key
    }

    suspend fun remove(connectionKey: String) {
        databaseConnector.transaction {
            SavedSqlConnections.deleteWhere { SavedSqlConnections.connectionKey eq connectionKey }
        }
        secretStore.deletePassword(connectionKey)
    }

    suspend fun getPassword(connectionKey: String): String? =
        secretStore.getPassword(connectionKey)

    suspend fun migrateFromLegacy(legacyConnections: List<ScreenStateSettings.SqlSavedConnection>): Int {
        if (legacyConnections.isEmpty()) return 0

        var migrated = 0
        legacyConnections.forEach { legacy ->
            val name = legacy.name.trim().ifBlank {
                buildString {
                    append(legacy.host.trim())
                    if (legacy.database.isNotBlank()) {
                        append("/")
                        append(legacy.database.trim())
                    }
                }.ifBlank { "connection" }
            }
            upsert(
                name = name,
                databaseType = legacy.databaseType,
                host = legacy.host,
                port = legacy.port,
                database = legacy.database,
                schema = legacy.schema,
                user = legacy.user,
                password = legacy.password,
                authDatabase = legacy.authDatabase
            )
            migrated++
        }
        return migrated
    }
}

