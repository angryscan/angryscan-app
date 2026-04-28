package org.angryscan.app.scan.common.connectors

import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.ServerAddress
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.ObjectCounter
import org.bson.Document
import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings
import java.util.concurrent.TimeUnit

@Serializable
class ConnectorMongoDB(
    val host: String,
    val port: Int = 27017,
    val database: String,
    val user: String = "",
    val password: String = "",
    /** SCRAM auth database; when blank, [database] is used. */
    val authDatabase: String = "",
    val rowLimit: Int = 1000
) : IDatabaseConnector {

    companion object {
        private val logger = KotlinLogging.logger {}
        private val relaxedJson: JsonWriterSettings =
            JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build()
    }

    internal fun mongoClient(): MongoClient =
        MongoClients.create(mongoClientSettings())

    private fun mongoClientSettings(): MongoClientSettings {
        val settingsBuilder = MongoClientSettings.builder()
            .applyToClusterSettings {
                it.hosts(listOf(ServerAddress(host, port)))
                it.serverSelectionTimeout(10, TimeUnit.SECONDS)
            }
            .applyToSocketSettings {
                it.connectTimeout(10, TimeUnit.SECONDS)
                it.readTimeout(30, TimeUnit.SECONDS)
            }
        if (user.isNotBlank()) {
            val authDb = authDatabase.ifBlank { database }
            settingsBuilder.credential(
                MongoCredential.createCredential(user, authDb, password.toCharArray())
            )
        }
        return settingsBuilder.build()
    }

    override suspend fun scanTables(
        path: String,
        tableSelected: (file: FoundedFile) -> Unit
    ): ObjectCounter = withContext(Dispatchers.IO) {
        val counter = ObjectCounter()
        val extraDatabases = path.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        val databasesToScan = (listOf(database) + extraDatabases).distinct()
        mongoClient().use { client ->
            for (dbName in databasesToScan) {
                val mongoDb = client.getDatabase(dbName)
                try {
                    val names = mongoDb.listCollectionNames().toList()
                    for (collName in names) {
                        if (collName.startsWith("system.")) continue
                        val estimated = try {
                            mongoDb.getCollection(collName).estimatedDocumentCount()
                        } catch (e: Exception) {
                            logger.warn(e) { "MongoDB estimatedDocumentCount failed for $dbName.$collName" }
                            0L
                        }
                        tableSelected(FoundedFile(path = "$dbName.$collName", size = estimated))
                        counter.add(estimated)
                    }
                } catch (e: Exception) {
                    logger.warn(e) { "MongoDB listCollections failed for database $dbName" }
                }
            }
        }
        counter
    }

    override suspend fun getTableContent(tablePath: String): String = withContext(Dispatchers.IO) {
        val (dbName, collName) = parseMongoCollectionPath(tablePath)
        mongoClient().use { client ->
            buildString {
                client.getDatabase(dbName).getCollection(collName)
                    .find()
                    .limit(rowLimit)
                    .forEach { doc ->
                        append(doc.toJson(relaxedJson))
                        append('\n')
                    }
            }
        }
    }

    override suspend fun getTableContentStructured(tablePath: String): List<Map<String, String>> =
        withContext(Dispatchers.IO) {
            val (dbName, collName) = parseMongoCollectionPath(tablePath)
            mongoClient().use { client ->
                buildList {
                    client.getDatabase(dbName).getCollection(collName)
                        .find()
                        .limit(rowLimit)
                        .forEach { doc ->
                            add(documentToStringRow(doc))
                        }
                }
            }
        }

    private fun documentToStringRow(doc: Document): Map<String, String> =
        doc.keys.associateWith { key -> bsonValueToScanString(doc[key]) }

    private fun bsonValueToScanString(value: Any?): String =
        when (value) {
            null -> ""
            is Document -> value.toJson(relaxedJson)
            is List<*> -> value.joinToString(separator = ",", prefix = "[", postfix = "]") { elem ->
                bsonValueToScanString(elem)
            }
            else -> value.toString()
        }

    override fun logSummary(): String =
        "Host: $host. Port: $port. Database: $database. Auth DB: ${authDatabase.ifBlank { database }}. Row limit: $rowLimit."

    override fun toString(): String = "ConnectorMongoDB"
}

/**
 * Split [tablePath] into database and collection name.
 * MongoDB forbids `.` in database names, so the first `.` separates DB from collection; the rest
 * belongs to the collection name (collections may contain dots).
 */
internal fun parseMongoCollectionPath(tablePath: String): Pair<String, String> {
    val idx = tablePath.indexOf('.')
    require(idx > 0 && idx < tablePath.length - 1) {
        "Collection path must be in database.collection format"
    }
    return tablePath.substring(0, idx) to tablePath.substring(idx + 1)
}
