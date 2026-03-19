package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.ObjectCounter
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

@Serializable
class ConnectorMySQL(
    val host: String,
    val port: Int = 3306,
    val database: String,
    val user: String,
    val password: String,
    val rowLimit: Int = 1000
) : IDatabaseConnector {

    private val jdbcUrl: String
        get() = "jdbc:mysql://$host:$port/$database"

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
                        val schema = resultSet.getString("table_schema")
                        val table = resultSet.getString("table_name")
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
                """SELECT * FROM ${escapeIdentifier(schema)}.${escapeIdentifier(table)} LIMIT ?"""
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
                """SELECT * FROM ${escapeIdentifier(schema)}.${escapeIdentifier(table)} LIMIT ?"""
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
        val schemaFilter = if (databases.isNotEmpty()) {
            "AND t.table_schema IN (${databases.joinToString(",") { "?" }})"
        } else {
            "AND t.table_schema NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')"
        }

        return """
            SELECT t.table_schema, t.table_name
            FROM information_schema.tables t
            WHERE t.table_type = 'BASE TABLE'
            $schemaFilter
            ORDER BY t.table_schema, t.table_name
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
        DriverManager.getConnection(jdbcUrl, user, password)

    /** MySQL uses backticks for identifier escaping. */
    private fun escapeIdentifier(value: String): String =
        "`" + value.replace("`", "``") + "`"

    private fun ResultSet.toJsonRow(): String {
        val metaData = metaData
        return buildString {
            append('{')
            for (index in 1..metaData.columnCount) {
                if (index > 1) append(',')
                append('"')
                append(metaData.getColumnLabel(index).escapeJson())
                append('"')
                append(':')
                append(getObject(index).toJsonValue())
            }
            append('}')
        }
    }

    private fun Any?.toJsonValue(): String =
        when (this) {
            null -> "null"
            is Number, is Boolean -> toString()
            else -> "\"${toString().escapeJson()}\""
        }

    private fun String.escapeJson(): String = buildString(length) {
        for (char in this@escapeJson) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }

    override fun toString(): String = "ConnectorMySQL"
}
