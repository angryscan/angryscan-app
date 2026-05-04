package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Validates ClickHouse connection parameters before creating a scan task.
 * Returns [DatabaseConnectionError] with field hint on failure, null on success.
 */
internal object ClickHouseConnectionValidator {

    suspend fun validate(
        host: String,
        port: Int,
        database: String,
        user: String,
        password: String
    ): DatabaseConnectionError? = withContext(Dispatchers.IO) {
        val jdbcUrl = "jdbc:clickhouse://$host:$port/$database"
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
                message = sanitizedConnectionErrorMessage(DatabaseConnectionErrorField.HOST)
            )
        }
    }

    /** Exposed for unit testing error message mapping. */
    internal fun parseConnectionError(e: SQLException): DatabaseConnectionError {
        val msg = (e.message ?: "").lowercase()
        val field = when {
            // CH intentionally does not distinguish wrong user vs. wrong password in the
            // message body; both surface as AUTHENTICATION_FAILED (code 516).
            msg.contains("authentication_failed") ||
            msg.contains("authentication failed") ||
            msg.contains("password is incorrect") ||
            msg.contains("wrong password") ||
            msg.contains("required_password") ->
                DatabaseConnectionErrorField.USER_PASSWORD
            msg.contains("database") && (msg.contains("doesn't exist") || msg.contains("does not exist")) ->
                DatabaseConnectionErrorField.DATABASE
            msg.contains("connection refused") ||
            msg.contains("could not connect") ||
            msg.contains("connection timed out") ||
            msg.contains("unknown host") ||
            msg.contains("no route to host") ->
                DatabaseConnectionErrorField.HOST
            else ->
                DatabaseConnectionErrorField.HOST
        }
        return DatabaseConnectionError(
            field = field,
            message = sanitizedConnectionErrorMessage(field)
        )
    }
}
