package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Validates GreenPlum connection parameters before creating a scan task.
 * GreenPlum uses the PostgreSQL wire protocol, so the PostgreSQL JDBC driver is reused.
 * Returns [DatabaseConnectionError] with field hint on failure, null on success.
 */
internal object GreenPlumConnectionValidator {

    suspend fun validate(
        host: String,
        port: Int,
        database: String,
        user: String,
        password: String
    ): DatabaseConnectionError? = withContext(Dispatchers.IO) {
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
            DatabaseConnectionError(
                field = DatabaseConnectionErrorField.HOST,
                message = e.message ?: "Connection failed"
            )
        }
    }

    internal fun parseConnectionError(e: SQLException): DatabaseConnectionError {
        val msg = (e.message ?: "").lowercase()
        val field = when {
            msg.contains("password authentication failed") ||
            msg.contains("authentication failed") ||
            msg.contains("no password was provided") ||
            msg.contains("password authentication failed for user") ->
                DatabaseConnectionErrorField.USER_PASSWORD
            msg.contains("connection refused") ||
            msg.contains("could not connect") ||
            msg.contains("connection timed out") ||
            msg.contains("could not translate host") ->
                DatabaseConnectionErrorField.HOST
            msg.contains("database") && msg.contains("does not exist") ->
                DatabaseConnectionErrorField.DATABASE
            msg.contains("role") && msg.contains("does not exist") ->
                DatabaseConnectionErrorField.USER
            else ->
                DatabaseConnectionErrorField.HOST
        }
        return DatabaseConnectionError(field = field, message = e.message ?: "Connection failed")
    }
}
