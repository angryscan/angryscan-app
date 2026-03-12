package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Validates PostgreSQL connection parameters before creating a scan task.
 * Returns [PostgresConnectionError] with field hint on failure, null on success.
 */
object PostgresConnectionValidator {

    suspend fun validate(
        host: String,
        port: Int,
        database: String,
        user: String,
        password: String
    ): PostgresConnectionError? = withContext(Dispatchers.IO) {
        val jdbcUrl = "jdbc:postgresql://$host:$port/$database"
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
            PostgresConnectionError(
                field = PostgresConnectionErrorField.HOST,
                message = e.message ?: "Connection failed"
            )
        }
    }

    /** Exposed for unit testing error message mapping. */
    internal fun parseConnectionError(e: SQLException): PostgresConnectionError {
        val msg = (e.message ?: "").lowercase()
        val field = when {
            msg.contains("password authentication failed") ||
            msg.contains("authentication failed") ||
            msg.contains("no password was provided") ||
            msg.contains("password authentication failed for user") ->
                PostgresConnectionErrorField.USER_PASSWORD
            msg.contains("connection refused") ||
            msg.contains("could not connect") ||
            msg.contains("connection timed out") ||
            msg.contains("could not translate host") ->
                PostgresConnectionErrorField.HOST
            msg.contains("database") && msg.contains("does not exist") ->
                PostgresConnectionErrorField.DATABASE
            msg.contains("role") && msg.contains("does not exist") ->
                PostgresConnectionErrorField.USER
            else ->
                PostgresConnectionErrorField.HOST
        }
        return PostgresConnectionError(field = field, message = e.message ?: "Connection failed")
    }
}
