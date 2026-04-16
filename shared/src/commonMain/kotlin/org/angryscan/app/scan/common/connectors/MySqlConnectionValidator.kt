package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Validates MySQL connection parameters before creating a scan task.
 * Returns [DatabaseConnectionError] with field hint on failure, null on success.
 */
internal object MySqlConnectionValidator {

    suspend fun validate(
        host: String,
        port: Int,
        database: String,
        user: String,
        password: String
    ): DatabaseConnectionError? = withContext(Dispatchers.IO) {
        val jdbcUrl = "jdbc:mysql://$host:$port/$database"
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
            msg.contains("access denied") ||
            msg.contains("authentication failed") ||
            msg.contains("password") && msg.contains("failed") ->
                DatabaseConnectionErrorField.USER_PASSWORD
            msg.contains("connection refused") ||
            msg.contains("could not connect") ||
            msg.contains("connection timed out") ||
            msg.contains("unknown host") ->
                DatabaseConnectionErrorField.HOST
            msg.contains("unknown database") ->
                DatabaseConnectionErrorField.DATABASE
            msg.contains("access denied for user") ->
                DatabaseConnectionErrorField.USER
            else ->
                DatabaseConnectionErrorField.HOST
        }
        return DatabaseConnectionError(field = field, message = e.message ?: "Connection failed")
    }
}
