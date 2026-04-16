package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Validates SQLite connection (file path) before creating a scan task.
 * Returns [DatabaseConnectionError] with field hint on failure, null on success.
 */
internal object SqliteConnectionValidator {

    suspend fun validate(filePath: String): DatabaseConnectionError? = withContext(Dispatchers.IO) {
        if (filePath.isBlank()) {
            return@withContext DatabaseConnectionError(
                field = DatabaseConnectionErrorField.FILE_PATH,
                message = "File path is required"
            )
        }
        val file = File(filePath)
        if (!file.exists()) {
            return@withContext DatabaseConnectionError(
                field = DatabaseConnectionErrorField.FILE_PATH,
                message = "File does not exist: $filePath"
            )
        }
        if (!file.isFile) {
            return@withContext DatabaseConnectionError(
                field = DatabaseConnectionErrorField.FILE_PATH,
                message = "Path is not a file: $filePath"
            )
        }
        val jdbcUrl = "jdbc:sqlite:$filePath"
        try {
            DriverManager.getConnection(jdbcUrl).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT 1").use { it.next() }
                }
            }
            null
        } catch (e: SQLException) {
            DatabaseConnectionError(
                field = DatabaseConnectionErrorField.FILE_PATH,
                message = e.message ?: "Failed to open database"
            )
        } catch (e: Exception) {
            DatabaseConnectionError(
                field = DatabaseConnectionErrorField.FILE_PATH,
                message = e.message ?: "Connection failed"
            )
        }
    }
}
