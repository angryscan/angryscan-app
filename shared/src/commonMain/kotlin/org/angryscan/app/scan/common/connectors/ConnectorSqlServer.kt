package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.ObjectCounter
import java.sql.Connection
import java.sql.ResultSetMetaData

/**
 * Microsoft SQL Server via JDBC. TLS: encrypted connection with trustServerCertificate for typical
 * lab or self-signed servers (see [SqlServerConnectionSupport]).
 */
@Serializable
class ConnectorSqlServer(
    val host: String,
    val port: Int = 1433,
    val database: String,
    val user: String,
    val password: String,
    val rowLimit: Int = 1000
) : IDatabaseConnector {

    override suspend fun scanTables(
        path: String,
        tableSelected: (file: FoundedFile) -> Unit
    ): ObjectCounter = withContext(Dispatchers.IO) {
        val filesCounter = ObjectCounter()
        openConnection().use { connection ->
            val schemas = if (path.isBlank()) emptyList() else path.split(";").map { it.trim() }.filter { it.isNotEmpty() }
            val query = buildTablesQuery(schemas)

            connection.prepareStatement(query).use { statement ->
                if (schemas.isNotEmpty()) {
                    schemas.forEachIndexed { index, schema ->
                        statement.setString(index + 1, schema)
                    }
                }

                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val schema = resultSet.getString("TABLE_SCHEMA")
                        val table = resultSet.getString("TABLE_NAME")
                        val rowCount = getRowCount(connection, schema, table)

                        tableSelected(
                            FoundedFile(
                                path = "$schema.$table",
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
        val (schema, table) = parseTablePath(tablePath)
        openConnection().use { connection ->
            connection.prepareStatement(
                """SELECT TOP (?) * FROM ${escapeIdentifier(schema)}.${escapeIdentifier(table)}"""
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
        val (schema, table) = parseTablePath(tablePath)
        openConnection().use { connection ->
            connection.prepareStatement(
                """SELECT TOP (?) * FROM ${escapeIdentifier(schema)}.${escapeIdentifier(table)}"""
            ).use { statement ->
                statement.setInt(1, rowLimit)
                statement.executeQuery().use { resultSet ->
                    val metaData = resultSet.metaData
                    val columnLabels = uniqueColumnLabels(metaData)
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

    private fun buildTablesQuery(schemas: List<String>): String {
        val schemaFilter = if (schemas.isNotEmpty()) {
            "AND t.TABLE_SCHEMA IN (${schemas.joinToString(",") { "?" }})"
        } else {
            "AND UPPER(t.TABLE_SCHEMA) NOT IN ('SYS', 'INFORMATION_SCHEMA', 'GUEST')"
        }

        return """
            SELECT t.TABLE_SCHEMA, t.TABLE_NAME
            FROM INFORMATION_SCHEMA.TABLES t
            WHERE t.TABLE_TYPE = 'BASE TABLE'
            $schemaFilter
            ORDER BY t.TABLE_SCHEMA, t.TABLE_NAME
        """.trimIndent()
    }

    private fun getRowCount(connection: Connection, schema: String, table: String): Long {
        connection.createStatement().use { statement ->
            statement.executeQuery(
                """SELECT COUNT(*) FROM ${escapeIdentifier(schema)}.${escapeIdentifier(table)}"""
            ).use { resultSet ->
                return if (resultSet.next()) resultSet.getLong(1) else 0L
            }
        }
    }

    private fun parseTablePath(tablePath: String): Pair<String, String> {
        val separatorIndex = tablePath.indexOf('.')
        require(separatorIndex > 0 && separatorIndex < tablePath.length - 1) {
            "Table path must be in schema.table format"
        }
        return tablePath.substring(0, separatorIndex) to tablePath.substring(separatorIndex + 1)
    }

    private fun openConnection(): Connection =
        SqlServerConnectionSupport.openConnection(host, port, database, user, password)

    /**
     * Stable labels when metadata returns duplicate column names (e.g. SELECT * with joins).
     */
    private fun uniqueColumnLabels(metaData: ResultSetMetaData): List<String> {
        val columnCount = metaData.columnCount
        val perBaseCount = mutableMapOf<String, Int>()
        return List(columnCount) { i ->
            val index = i + 1
            val base = metaData.getColumnLabel(index).ifBlank { "COLUMN_$index" }
            val next = (perBaseCount[base] ?: 0) + 1
            perBaseCount[base] = next
            if (next == 1) base else "$base#$next"
        }
    }

    /** SQL Server bracket quoting for identifiers. */
    private fun escapeIdentifier(value: String): String =
        "[" + value.replace("]", "]]") + "]"

    override fun logSummary(): String =
        "Host: $host. Port: $port. Database: $database. Row limit: $rowLimit."

    override fun toString(): String = "ConnectorSqlServer"
}
