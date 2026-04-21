package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.ObjectCounter
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

@Serializable
class ConnectorClickHouse(
    val host: String,
    val port: Int = 8123,
    val database: String,
    val user: String,
    val password: String,
    val rowLimit: Int = 1000
) : IDatabaseConnector {

    private val jdbcUrl: String
        get() = "jdbc:clickhouse://$host:$port/$database"

    override suspend fun scanTables(
        path: String,
        tableSelected: (file: FoundedFile) -> Unit
    ): ObjectCounter = withContext(Dispatchers.IO) {
        val filesCounter = ObjectCounter()
        openConnection().use { connection ->
            val databases = if (path.isBlank()) emptyList() else path.split(";").map { it.trim() }.filter { it.isNotEmpty() }
            val query = buildTablesQuery(databases)

            connection.prepareStatement(query).use { statement ->
                if (databases.isNotEmpty()) {
                    databases.forEachIndexed { index, db ->
                        statement.setString(index + 1, db)
                    }
                }

                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val db = resultSet.getString("database")
                        val table = resultSet.getString("name")
                        val rowCount = getRowCount(connection, db, table)

                        tableSelected(
                            FoundedFile(
                                path = "$db.$table",
                                size = rowCount
                            )
                        )
                        filesCounter.add(rowCount)
                    }
                }
            }
        }
        filesCounter
    }

    override suspend fun getTableContent(tablePath: String): String = withContext(Dispatchers.IO) {
        val (db, table) = parseTablePath(tablePath)
        openConnection().use { connection ->
            connection.prepareStatement(
                """SELECT * FROM ${escapeIdentifier(db)}.${escapeIdentifier(table)} LIMIT ?"""
            ).use { statement ->
                statement.setInt(1, rowLimit)
                statement.executeQuery().use { resultSet ->
                    buildString {
                        while (resultSet.next()) {
                            append(resultSet.toJsonRow())
                            append('\n')
                        }
                    }
                }
            }
        }
    }

    override suspend fun getTableContentStructured(tablePath: String): List<Map<String, String>> = withContext(Dispatchers.IO) {
        val (db, table) = parseTablePath(tablePath)
        openConnection().use { connection ->
            connection.prepareStatement(
                """SELECT * FROM ${escapeIdentifier(db)}.${escapeIdentifier(table)} LIMIT ?"""
            ).use { statement ->
                statement.setInt(1, rowLimit)
                statement.executeQuery().use { resultSet ->
                    val metaData = resultSet.metaData
                    val columnCount = metaData.columnCount
                    val columnLabels = (1..columnCount).map { metaData.getColumnLabel(it) }
                    buildList {
                        while (resultSet.next()) {
                            add(columnLabels.associateWith { col ->
                                resultSet.getString(col) ?: ""
                            })
                        }
                    }
                }
            }
        }
    }

    private fun buildTablesQuery(databases: List<String>): String {
        // ClickHouse does not support boolean filter on is_temporary via JDBC consistently;
        // exclude system databases and views via engine.
        val dbFilter = if (databases.isNotEmpty()) {
            "AND database IN (${databases.joinToString(",") { "?" }})"
        } else {
            "AND database NOT IN ('system', 'INFORMATION_SCHEMA', 'information_schema')"
        }

        return """
            SELECT database, name
            FROM system.tables
            WHERE is_temporary = 0
              AND engine NOT LIKE '%View%'
              $dbFilter
            ORDER BY database, name
        """.trimIndent()
    }

    private fun getRowCount(connection: Connection, db: String, table: String): Long {
        connection.createStatement().use { statement ->
            statement.executeQuery(
                """SELECT count() FROM ${escapeIdentifier(db)}.${escapeIdentifier(table)}"""
            ).use { resultSet ->
                return if (resultSet.next()) resultSet.getLong(1) else 0L
            }
        }
    }

    private fun parseTablePath(tablePath: String): Pair<String, String> {
        val separatorIndex = tablePath.indexOf('.')
        require(separatorIndex > 0 && separatorIndex < tablePath.length - 1) {
            "Table path must be in database.table format"
        }
        return tablePath.substring(0, separatorIndex) to tablePath.substring(separatorIndex + 1)
    }

    private fun openConnection(): Connection {
        // Both timeouts are milliseconds in the clickhouse-jdbc client options.
        // Note: property name is `connection_timeout` (not `connect_timeout`); unknown
        // keys are silently ignored by ClickHouseClient.
        val props = Properties().apply {
            setProperty("user", user)
            setProperty("password", password)
            setProperty("connection_timeout", "10000")
            setProperty("socket_timeout", "30000")
        }
        return DriverManager.getConnection(jdbcUrl, props)
    }

    /** ClickHouse uses backticks for identifier escaping. */
    private fun escapeIdentifier(value: String): String =
        "`" + value.replace("`", "``") + "`"

    override fun logSummary(): String =
        "Host: $host. Port: $port. Database: $database. Row limit: $rowLimit."

    override fun toString(): String = "ConnectorClickHouse"
}
