package org.angryscan.app.scan.common.connectors

import com.mongodb.client.MongoClients
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.junit.AfterClass
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration test against MongoDB in Docker (Testcontainers).
 * Requires Docker. Skipped when the container is not running.
 */
internal class ConnectorMongoDBIntegrationTest {

    companion object {
        private val mongo: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("mongo:7"))
                .withExposedPorts(27017)

        @BeforeClass
        @JvmStatic
        fun startContainer() {
            try {
                mongo.start()
                val uri = "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}/testdb"
                MongoClients.create(uri).use { client ->
                    val coll = client.getDatabase("testdb").getCollection("contacts")
                    coll.drop()
                    coll.insertOne(Document("email", "a@b.c").append("name", "Alice"))
                    val dotted = client.getDatabase("testdb").getCollection("events.v1")
                    dotted.drop()
                    dotted.insertOne(Document("kind", "click"))
                }
            } catch (e: Exception) {
                System.err.println("Failed to start MongoDB container: ${e.message}")
            }
        }

        @AfterClass
        @JvmStatic
        fun stopContainer() {
            if (mongo.isRunning) {
                mongo.stop()
            }
        }
    }

    private lateinit var connector: ConnectorMongoDB

    @Before
    fun setUp() {
        assumeTrue("Docker is not available – skipping integration test", mongo.isRunning)
        connector = ConnectorMongoDB(
            host = mongo.host,
            port = mongo.getMappedPort(27017),
            database = "testdb"
        )
    }

    @Test
    fun `scanTables discovers contacts collection`() = runBlocking {
        val found = mutableListOf<FoundedFile>()
        connector.scanTables("") { found.add(it) }
        assertTrue(found.any { it.path == "testdb.contacts" }, "found: $found")
    }

    @Test
    fun `getTableContentStructured returns row`() = runBlocking {
        val rows = connector.getTableContentStructured("testdb.contacts")
        assertTrue(rows.isNotEmpty())
        assertEquals("a@b.c", rows[0]["email"])
    }

    @Test
    fun `collection name may contain dots after first path dot`() = runBlocking {
        val rows = connector.getTableContentStructured("testdb.events.v1")
        assertTrue(rows.isNotEmpty())
        assertEquals("click", rows[0]["kind"])
    }

    @Test
    fun `extra databases path merges with default database`() = runBlocking {
        val uri = "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}/otherdb"
        MongoClients.create(uri).use { client ->
            client.getDatabase("otherdb").getCollection("logs").insertOne(Document("msg", "hi"))
        }
        val found = mutableListOf<FoundedFile>()
        connector.scanTables("otherdb") { found.add(it) }
        assertTrue(found.any { it.path == "otherdb.logs" }, "found: $found")
    }
}
