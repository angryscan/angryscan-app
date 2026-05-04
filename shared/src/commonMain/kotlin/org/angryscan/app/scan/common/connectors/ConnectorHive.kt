package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.ObjectCounter
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

@Serializable
class ConnectorHive(
    val host: String,
    val port: Int = 10000,
    val database: String,
    val user: String,
    val password: String,
    val rowLimit: Int = 1000
) : IDatabaseConnector {

    private val jdbcUrl: String
        get() = "jdbc:hive2://$host:$port/$database"

    override suspend fun scanTables(
        path: String,
        tableSelected: (file: FoundedFile) -> Unit
    ): ObjectCounter = withContext(Dispatchers.IO) {
        val filesCounter = ObjectCounter()
        openConnection().use { connection ->
            val databases = if (path.isBlank()) listOf(database)
                else path.split(";").map { it.trim() }.filter { it.isNotEmpty() }

            for (db in databases) {
                val rs = connection.metaData.getTables(db, null, "%", null)
                rs.use { resultSet ->
                    while (resultSet.next()) {
                        val schema = resultSet.getString("TABLE_CAT")?.takeIf { it.isNotEmpty() } ?: db
                        val table = resultSet.getString("TABLE_NAME")

                        tableSelected(
                            FoundedFile(
                                path = "$schema.$table",
                                size = 0L
                            )
                        )
                        filesCounter.add(0L)
                    }
                }
            }
        }
        filesCounter
    }

    override suspend fun getTableContent(tablePath: String): String = withContext(Dispatchers.IO) {
        val (schema, table) = parseTablePath(tablePath)
        openConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT * FROM ${escapeIdentifier(schema)}.${escapeIdentifier(table)} LIMIT $rowLimit"
                ).use { resultSet ->
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
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT * FROM ${escapeIdentifier(schema)}.${escapeIdentifier(table)} LIMIT $rowLimit"
                ).use { resultSet ->
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

    private fun parseTablePath(tablePath: String): Pair<String, String> {
        val separatorIndex = tablePath.indexOf('.')
        require(separatorIndex > 0 && separatorIndex < tablePath.length - 1) {
            "Table path must be in database.table format"
        }
        return tablePath.substring(0, separatorIndex) to tablePath.substring(separatorIndex + 1)
    }

    private fun openConnection(): Connection {
        val props = Properties().apply {
            setProperty("user", user)
            setProperty("password", password)
        }
        return DriverManager.getConnection(jdbcUrl, props)
    }

    /** Hive uses backticks for identifier escaping. */
    private fun escapeIdentifier(value: String): String =
        "`" + value.replace("`", "``") + "`"

    override fun logSummary(): String =
        "Host: $host. Port: $port. Database: $database. Row limit: $rowLimit."

    override fun toString(): String = "ConnectorHive"
}
