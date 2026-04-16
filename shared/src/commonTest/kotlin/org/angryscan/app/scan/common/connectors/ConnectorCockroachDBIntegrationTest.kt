package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.runBlocking
import org.angryscan.app.common.MatchersRegister
import org.angryscan.app.scan.DatabaseContentScanner
import org.angryscan.app.scan.engine.ScanEnginesFactory
import org.angryscan.common.engine.IScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.angryscan.gitleaks.matcher.GitleaksMatcher
import org.junit.AfterClass
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.sql.DriverManager
import java.time.Duration
import kotlin.test.assertTrue

/**
 * Integration test against a CockroachDB instance managed by Testcontainers.
 * Requires Docker to be running. The container is started once for all tests
 * and initialized with test data from cockroachdb/init-test-cockroachdb.sql.
 *
 * Uses cockroachdb/cockroach image in single-node insecure mode.
 * Default credentials: root (no password), database: defaultdb, port: 26257.
 */
internal class ConnectorCockroachDBIntegrationTest {

    companion object {
        private const val CRDB_PORT = 26257
        private const val CRDB_USER = "root"
        private const val CRDB_PASSWORD = ""
        private const val CRDB_DATABASE = "defaultdb"

        @Suppress("DEPRECATION")
        private val cockroach: GenericContainer<*> = GenericContainer("cockroachdb/cockroach:v24.3.5")
            .withExposedPorts(CRDB_PORT)
            .withCommand("start-single-node", "--insecure")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(120)))

        @BeforeClass
        @JvmStatic
        fun startContainer() {
            try {
                cockroach.start()
                initDatabase()
            } catch (e: Exception) {
                System.err.println("Failed to start CockroachDB container: ${e.message}")
            }
        }

        private fun initDatabase() {
            if (!cockroach.isRunning) return
            val jdbcUrl = "jdbc:postgresql://${cockroach.host}:${cockroach.getMappedPort(CRDB_PORT)}/$CRDB_DATABASE?sslmode=disable"
            val initSql = ConnectorCockroachDBIntegrationTest::class.java
                .classLoader
                .getResource("cockroachdb/init-test-cockroachdb.sql")!!
                .readText()

            var lastException: Exception? = null
            for (attempt in 1..20) {
                try {
                    DriverManager.getConnection(jdbcUrl, CRDB_USER, CRDB_PASSWORD).use { conn ->
                        conn.createStatement().use { stmt ->
                            for (sql in initSql.split(";").map { it.trim() }.filter { it.isNotEmpty() }) {
                                stmt.execute(sql)
                            }
                        }
                    }
                    return
                } catch (e: Exception) {
                    lastException = e
                    Thread.sleep(2000)
                }
            }
            throw lastException ?: IllegalStateException("initDatabase failed")
        }

        @AfterClass
        @JvmStatic
        fun stopContainer() {
            cockroach.stop()
        }
    }

    private lateinit var connector: ConnectorCockroachDB

    @Before
    fun setUp() {
        assumeTrue("Docker is not available – skipping integration test", cockroach.isRunning)
        connector = ConnectorCockroachDB(
            host = cockroach.host,
            port = cockroach.getMappedPort(CRDB_PORT),
            database = CRDB_DATABASE,
            user = CRDB_USER,
            password = CRDB_PASSWORD
        )
    }

    @Test
    fun `scanTables discovers at least one table`() = runBlocking {
        val tables = mutableListOf<FoundedFile>()
        connector.scanTables(path = "") { tables.add(it) }
        assertTrue(tables.isNotEmpty(), "Expected at least one table in the test database")
    }

    @Test
    fun `scanTables discovers all 9 expected tables`() = runBlocking {
        val tables = mutableListOf<FoundedFile>()
        connector.scanTables(path = "") { tables.add(it) }
        assertTrue(tables.size >= 9, "Expected at least 9 tables, found ${tables.size}")
    }

    @Test
    fun `getTableContentStructured returns rows within rowLimit`() = runBlocking {
        val tables = mutableListOf<FoundedFile>()
        connector.scanTables(path = "") { tables.add(it) }
        assumeTrue("No tables found", tables.isNotEmpty())

        for (table in tables) {
            val rows = connector.getTableContentStructured(table.path)
            assertTrue(rows.size <= 1000, "Row count exceeds rowLimit for ${table.path}: ${rows.size}")
        }
    }

    @Test
    fun `full scan pipeline completes for every table`() = runBlocking {
        val tables = mutableListOf<FoundedFile>()
        connector.scanTables(path = "") { tables.add(it) }
        assumeTrue("No tables found", tables.isNotEmpty())

        GitleaksMatcher.init()
        val engines: List<IScanEngine> = ScanEnginesFactory.build(
            preferredEngine = KotlinEngine::class,
            matchers = MatchersRegister.toList(),
            requireKeywords = false
        )
        try {
            for (table in tables) {
                val rows = connector.getTableContentStructured(table.path)
                val result = DatabaseContentScanner.scanContentStructured(
                    path = table.path,
                    rows = rows,
                    engines = engines
                )
                assertTrue(!result.skipped(), "Scan should not be skipped for ${table.path}")
                assertTrue(result.columnFields.isNotEmpty(), "Scan should have results for ${table.path}")
            }
        } finally {
            engines.forEach { it.close() }
            GitleaksMatcher.close()
        }
    }

    @Test
    fun `scanTables callback is invoked synchronously before return`() = runBlocking {
        val callbackOrder = mutableListOf<String>()
        connector.scanTables(path = "") {
            callbackOrder.add("table:${it.path}")
        }
        callbackOrder.add("after_scanTables")

        assertTrue(
            callbackOrder.last() == "after_scanTables",
            "scanTables must invoke all callbacks before returning"
        )
        assertTrue(
            callbackOrder.size > 1,
            "Expected at least one table callback"
        )
    }
}
