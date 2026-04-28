package org.angryscan.app.scan.common.connectors

import com.mongodb.MongoException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document

/**
 * Validates MongoDB connection parameters before creating a scan task.
 */
internal object MongoConnectionValidator {

    suspend fun validate(
        host: String,
        port: Int,
        database: String,
        user: String,
        password: String,
        authDatabase: String = ""
    ): DatabaseConnectionError? = withContext(Dispatchers.IO) {
        val connector = ConnectorMongoDB(
            host = host,
            port = port,
            database = database,
            user = user,
            password = password,
            authDatabase = authDatabase
        )
        try {
            connector.mongoClient().use { client ->
                client.getDatabase(database).runCommand(Document("ping", 1))
            }
            null
        } catch (e: MongoException) {
            parseMongoError(e)
        } catch (_: Exception) {
            DatabaseConnectionError(
                field = DatabaseConnectionErrorField.HOST,
                message = sanitizedConnectionErrorMessage(DatabaseConnectionErrorField.HOST)
            )
        }
    }

    private fun parseMongoError(e: MongoException): DatabaseConnectionError {
        val msg = (e.message ?: "").lowercase()
        val field = when {
            msg.contains("authentication failed") ||
                msg.contains("bad auth") ||
                msg.contains("unauthorized") ||
                msg.contains("not authorized") ->
                DatabaseConnectionErrorField.USER_PASSWORD
            msg.contains("ns not found") ->
                DatabaseConnectionErrorField.DATABASE
            else ->
                DatabaseConnectionErrorField.HOST
        }
        return DatabaseConnectionError(
            field = field,
            message = sanitizedConnectionErrorMessage(field)
        )
    }
}
