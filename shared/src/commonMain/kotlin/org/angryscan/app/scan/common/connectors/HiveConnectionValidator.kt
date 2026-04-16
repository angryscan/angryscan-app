package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Validates Hive (HiveServer2) connection parameters before creating a scan task.
 * Uses the Hive JDBC driver with `jdbc:hive2://` URL scheme.
 * Returns [DatabaseConnectionError] with field hint on failure, null on success.
 */
internal object HiveConnectionValidator {

    suspend fun validate(
        host: String,
        port: Int,
        database: String,
        user: String,
        password: String
    ): DatabaseConnectionError? = withContext(Dispatchers.IO) {
        val jdbcUrl = "jdbc:hive2://$host:$port/$database"
        try {
            DriverManager.getConnection(jdbcUrl, user, password).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT 1").use { it.next() }
                }
            }
            null
        } catch (e: SQLException) {
            parseConnectionError(e)
        } catch (e: Exception) {
            DatabaseConnectionError(
                field = DatabaseConnectionErrorField.HOST,
                message = e.message ?: "Connection failed"
            )
        }
    }

    internal fun parseConnectionError(e: SQLException): DatabaseConnectionError {
        val msg = (e.message ?: "").lowercase()
        val field = when {
            msg.contains("authentication") ||
            msg.contains("login failed") ||
            msg.contains("access denied") ->
                DatabaseConnectionErrorField.USER_PASSWORD
            msg.contains("connection refused") ||
            msg.contains("could not connect") ||
            msg.contains("connection timed out") ||
            msg.contains("could not open client transport") ||
            msg.contains("unknown host") ->
                DatabaseConnectionErrorField.HOST
            msg.contains("database") && msg.contains("not found") ->
                DatabaseConnectionErrorField.DATABASE
            else ->
                DatabaseConnectionErrorField.HOST
        }
        return DatabaseConnectionError(field = field, message = e.message ?: "Connection failed")
    }
}
