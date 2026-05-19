package org.angryscan.app.scan.common.connectors

import com.mongodb.MongoException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document

/**
 * Validates MongoDB connection parameters before creating a scan task.
 * Returns [DatabaseConnectionError] with field hint on failure, null on success.
 */
internal object MongoConnectionValidator {

    suspend fun validate(
        host: String,
        port: Int,
        database: String,
        user: String,
        password: String,
    ): DatabaseConnectionError? = withContext(Dispatchers.IO) {
        try {
            createMongoClient(host, port, database, user, password).use { client ->
                client.getDatabase(database).runCommand(Document("ping", 1))
            }
            null
        } catch (e: MongoException) {
            parseConnectionError(e)
        } catch (e: Exception) {
            DatabaseConnectionError(
                field = DatabaseConnectionErrorField.HOST,
                message = sanitizedConnectionErrorMessage(DatabaseConnectionErrorField.HOST),
            )
        }
    }

    internal fun parseConnectionError(e: MongoException): DatabaseConnectionError {
        val msg = (e.message ?: "").lowercase()
        val field = when {
            msg.contains("authentication failed") ||
                msg.contains("bad auth") ||
                msg.contains("invalid credentials") ->
                DatabaseConnectionErrorField.USER_PASSWORD
            msg.contains("timed out") ||
                msg.contains("connection refused") ||
                msg.contains("couldn't connect") ->
                DatabaseConnectionErrorField.HOST
            msg.contains("database name") && msg.contains("invalid") ->
                DatabaseConnectionErrorField.DATABASE
            else -> DatabaseConnectionErrorField.HOST
        }
        return DatabaseConnectionError(
            field = field,
            message = sanitizedConnectionErrorMessage(field),
        )
    }
}
