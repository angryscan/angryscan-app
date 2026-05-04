package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties

/**
 * Validates Amazon Redshift connection parameters before creating a scan task.
 * Uses the Redshift JDBC driver (jdbc:redshift URL). Returns [DatabaseConnectionError] on failure, null on success.
 */
internal object RedshiftConnectionValidator {

    suspend fun validate(
        host: String,
        port: Int,
        database: String,
        user: String,
        password: String
    ): DatabaseConnectionError? = withContext(Dispatchers.IO) {
        val jdbcUrl = "jdbc:redshift://$host:$port/$database"
        val props = Properties().apply {
            setProperty("user", user)
            setProperty("password", password)
            setProperty("connectTimeout", "10")
            setProperty("socketTimeout", "30")
        }
        try {
            DriverManager.getConnection(jdbcUrl, props).use { conn ->
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
            msg.contains("password authentication failed") ||
                msg.contains("authentication failed") ||
                msg.contains("no password was provided") ||
                msg.contains("password authentication failed for user") ||
                msg.contains("invalid username or password") ->
                DatabaseConnectionErrorField.USER_PASSWORD
            msg.contains("connection refused") ||
                msg.contains("could not connect") ||
                msg.contains("connection timed out") ||
                msg.contains("could not translate host") ||
                msg.contains("unable to connect") ->
                DatabaseConnectionErrorField.HOST
            msg.contains("database") && msg.contains("does not exist") ->
                DatabaseConnectionErrorField.DATABASE
            (msg.contains("role") && msg.contains("does not exist")) ||
                (msg.contains("user") && msg.contains("does not exist")) ->
                DatabaseConnectionErrorField.USER
            else ->
                DatabaseConnectionErrorField.HOST
        }
        return DatabaseConnectionError(
            field = field,
            message = sanitizedConnectionErrorMessage(field)
        )
    }
}
