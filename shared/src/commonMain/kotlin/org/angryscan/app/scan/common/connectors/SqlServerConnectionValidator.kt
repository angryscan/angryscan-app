package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.SQLException

/**
 * Validates Microsoft SQL Server connection parameters before creating a scan task.
 */
internal object SqlServerConnectionValidator {

    suspend fun validate(
        host: String,
        port: Int,
        database: String,
        user: String,
        password: String
    ): DatabaseConnectionError? = withContext(Dispatchers.IO) {
        try {
            SqlServerConnectionSupport.openConnection(host, port, database, user, password).use { conn ->
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

    internal fun parseConnectionError(e: SQLException): DatabaseConnectionError {
        val msg = (e.message ?: "").lowercase()
        val field = when {
            msg.contains("login failed") ||
                msg.contains("authentication failed") ||
                (msg.contains("password") && msg.contains("failed")) ->
                DatabaseConnectionErrorField.USER_PASSWORD
            msg.contains("cannot open database") ||
                msg.contains("unable to open") ->
                DatabaseConnectionErrorField.DATABASE
            msg.contains("connection refused") ||
                msg.contains("connection timed out") ||
                msg.contains("timeout") ||
                msg.contains("unknown host") ||
                msg.contains("could not connect") ->
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
