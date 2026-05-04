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
 * Integration test against a HiveServer2 instance managed by Testcontainers.
 * Requires Docker to be running. The container is started once for all tests
 * and initialized with test data from hive/init-test-hive.hql via JDBC.
 *
 * Uses apache/hive:4.0.0 image with embedded metastore (Derby).
 * Default credentials: hive / (empty), database: default, port: 10000.
 */
internal class ConnectorHiveIntegrationTest {

    companion object {
        private const val HIVE_PORT = 10000
        private const val HIVE_USER = "hive"
        private const val HIVE_PASSWORD = ""
        private const val HIVE_DATABASE = "default"

        @Suppress("DEPRECATION")
        private val hive: GenericContainer<*> = GenericContainer("apache/hive:4.0.0")
            .withExposedPorts(HIVE_PORT)
            .withEnv("SERVICE_NAME", "hiveserver2")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(180)))

        @BeforeClass
        @JvmStatic
        fun startContainer() {
            try {
                hive.start()
                initDatabase()
            } catch (e: Exception) {
                System.err.println("Failed to start Hive container: ${e.message}")
            }
        }

        private fun initDatabase() {
            if (!hive.isRunning) return
            val jdbcUrl = "jdbc:hive2://${hive.host}:${hive.getMappedPort(HIVE_PORT)}/$HIVE_DATABASE"
            val initHql = ConnectorHiveIntegrationTest::class.java
                .classLoader
                .getResource("hive/init-test-hive.hql")!!
                .readText()

            DriverManager.getConnection(jdbcUrl, HIVE_USER, HIVE_PASSWORD).use { conn ->
                conn.createStatement().use { stmt ->
                    for (sql in initHql.split(";").map { it.trim() }.filter { it.isNotEmpty() }) {
                        stmt.execute(sql)
                    }
                }
            }
        }

        @AfterClass
        @JvmStatic
        fun stopContainer() {
            hive.stop()
        }
    }

    private lateinit var connector: ConnectorHive

    @Before
    fun setUp() {
        assumeTrue("Docker is not available – skipping integration test", hive.isRunning)
        connector = ConnectorHive(
            host = hive.host,
            port = hive.getMappedPort(HIVE_PORT),
            database = HIVE_DATABASE,
            user = HIVE_USER,
            password = HIVE_PASSWORD
        )
    }

    @Test
    fun `scanTables discovers at least one table`() = runBlocking {
        val tables = mutableListOf<FoundedFile>()
        connector.scanTables(path = "") { tables.add(it) }
        assertTrue(tables.isNotEmpty(), "Expected at least one table in the test database")
    }

    @Test
    fun `scanTables discovers all 5 expected tables`() = runBlocking {
        val tables = mutableListOf<FoundedFile>()
        connector.scanTables(path = "") { tables.add(it) }
        assertTrue(tables.size >= 5, "Expected at least 5 tables, found ${tables.size}")
    }

    @Test
    fun `getTableContentStructured returns rows`() = runBlocking {
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
