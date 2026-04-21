package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.ObjectCounter
import java.sql.DriverManager

@Serializable
class ConnectorSqlite(
    val filePath: String,
    val rowLimit: Int = 1000
) : IDatabaseConnector {

    private val jdbcUrl: String
        get() = "jdbc:sqlite:$filePath"

    override suspend fun scanTables(
        path: String,
        tableSelected: (file: FoundedFile) -> Unit
    ): ObjectCounter = withContext(Dispatchers.IO) {
        val filesCounter = ObjectCounter()
        DriverManager.getConnection(jdbcUrl).use { connection ->
            val tables = if (path.isBlank()) {
                connection.createStatement().use { stmt ->
                    stmt.executeQuery(
                        """SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name"""
                    ).use { rs ->
                        buildList { while (rs.next()) add(rs.getString("name")) }
                    }
                }
            } else {
                path.split(";").map { it.trim() }.filter { it.isNotEmpty() }
            }

            tables.forEach { table ->
                val rowCount = getRowCount(connection, table)
                tableSelected(FoundedFile(path = table, size = rowCount))
                filesCounter.add(rowCount)
            }
        }
        filesCounter
    }

    override suspend fun getTableContent(tablePath: String): String = withContext(Dispatchers.IO) {
        DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.prepareStatement(
                """SELECT * FROM ${escapeIdentifier(tablePath)} LIMIT ?"""
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
        DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.prepareStatement(
                """SELECT * FROM ${escapeIdentifier(tablePath)} LIMIT ?"""
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

    private fun getRowCount(connection: java.sql.Connection, table: String): Long {
        connection.createStatement().use { statement ->
            statement.executeQuery(
                """SELECT COUNT(*) FROM ${escapeIdentifier(table)}"""
            ).use { resultSet ->
                return if (resultSet.next()) resultSet.getLong(1) else 0L
            }
        }
    }

    /** SQLite uses double quotes for identifier escaping. */
    private fun escapeIdentifier(value: String): String =
        "\"" + value.replace("\"", "\"\"") + "\""

    override fun logSummary(): String =
        "File: $filePath. Row limit: $rowLimit."

    override fun toString(): String = "ConnectorSqlite"
}
