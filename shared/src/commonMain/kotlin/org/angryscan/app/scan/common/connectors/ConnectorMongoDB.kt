package org.angryscan.app.scan.common.connectors

import com.mongodb.client.MongoClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.ObjectCounter
import org.bson.Document
import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings

/**
 * MongoDB connector using the official sync driver (collections as scan targets; BSON documents as rows).
 * [path] in [scanTables] is a semicolon-separated list of collection names; if empty, all non-system collections are listed.
 * Table paths use `database.collection` (same pattern as other DB connectors).
 */
@Serializable
class ConnectorMongoDB(
    val host: String,
    val port: Int = 27_017,
    val database: String,
    val user: String,
    val password: String,
    val rowLimit: Int = 1000,
) : IDatabaseConnector {

    companion object {
        private val jsonWriterSettings: JsonWriterSettings =
            JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build()
    }

    private fun openClient(): MongoClient =
        createMongoClient(host, port, database, user, password)

    override suspend fun scanTables(
        path: String,
        tableSelected: (file: FoundedFile) -> Unit,
    ): ObjectCounter = withContext(Dispatchers.IO) {
        val counter = ObjectCounter()
        openClient().use { client ->
            val db = client.getDatabase(database)
            val filters = path.split(";").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            val names = db.listCollectionNames()
                .asSequence()
                .filter { !it.startsWith("system.") && (filters.isEmpty() || it in filters) }
                .sorted()
                .toList()
            for (collName in names) {
                val count = db.getCollection(collName).estimatedDocumentCount()
                tableSelected(
                    FoundedFile(
                        path = "$database.$collName",
                        size = count,
                    ),
                )
                counter.add(count)
            }
        }
        counter
    }

    override suspend fun getTableContent(tablePath: String): String = withContext(Dispatchers.IO) {
        val collectionName = parseCollectionName(tablePath)
        openClient().use { client ->
            val coll = client.getDatabase(database).getCollection(collectionName)
            buildString {
                coll.find()
                    .limit(rowLimit)
                    .iterator()
                    .use { cursor ->
                        while (cursor.hasNext()) {
                            val doc = cursor.next()
                            append(doc.toJson(jsonWriterSettings))
                            append('\n')
                        }
                    }
            }
        }
    }

    override suspend fun getTableContentStructured(tablePath: String): List<Map<String, String>> =
        withContext(Dispatchers.IO) {
            val collectionName = parseCollectionName(tablePath)
            openClient().use { client ->
                val coll = client.getDatabase(database).getCollection(collectionName)
                buildList {
                    coll.find()
                        .limit(rowLimit)
                        .iterator()
                        .use { cursor ->
                            while (cursor.hasNext()) {
                                add(documentToFlatRow(cursor.next()))
                            }
                        }
                }
            }
        }

    private fun documentToFlatRow(doc: Document): Map<String, String> =
        doc.keys.associateWith { key -> bsonValueToCellString(doc[key]) }

    private fun bsonValueToCellString(value: Any?): String =
        when (value) {
            null -> ""
            is String -> value
            is Number, is Boolean -> value.toString()
            is Document -> value.toJson(jsonWriterSettings)
            is List<*> -> value.joinToString(separator = ",", prefix = "[", postfix = "]") { bsonValueToCellString(it) }
            else -> try {
                Document("v", value).toJson(jsonWriterSettings)
            } catch (_: Exception) {
                value.toString()
            }
        }

    private fun parseCollectionName(tablePath: String): String {
        val separatorIndex = tablePath.indexOf('.')
        require(separatorIndex > 0 && separatorIndex < tablePath.length - 1) {
            "Table path must be database.collection format"
        }
        val dbPart = tablePath.substring(0, separatorIndex)
        require(dbPart == database) {
            "Table path database must match connection database"
        }
        return tablePath.substring(separatorIndex + 1)
    }

    override fun logSummary(): String =
        "Host: $host. Port: $port. Database: $database. Row limit: $rowLimit."

    override fun toString(): String = "ConnectorMongoDB"
}
