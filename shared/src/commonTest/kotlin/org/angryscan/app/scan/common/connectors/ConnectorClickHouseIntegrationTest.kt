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
 * Integration test against a ClickHouse instance managed by Testcontainers.
 * Requires Docker to be running. The container is started once for all tests
 * and initialized with test data from clickhouse/init-test-clickhouse.sql.
 *
 * Uses clickhouse/clickhouse-server image. Default user: `default`, empty password,
 * HTTP port: 8123.
 */
internal class ConnectorClickHouseIntegrationTest {

    companion object {
        private const val CH_HTTP_PORT = 8123
        private const val CH_USER = "default"
        private const val CH_PASSWORD = ""
        private const val CH_DATABASE = "default"

        @Suppress("DEPRECATION")
        private val clickhouse: GenericContainer<*> = GenericContainer("clickhouse/clickhouse-server:24.8-alpine")
            .withExposedPorts(CH_HTTP_PORT)
            // Prevents the entrypoint from disabling network access for the default user
            // (it does so whenever CLICKHOUSE_USER/CLICKHOUSE_PASSWORD are not provided).
            .withEnv("CLICKHOUSE_SKIP_USER_SETUP", "1")
            .waitingFor(Wait.forHttp("/ping").forPort(CH_HTTP_PORT).withStartupTimeout(Duration.ofSeconds(120)))

        @BeforeClass
        @JvmStatic
        fun startContainer() {
            try {
                clickhouse.start()
                initDatabase()
            } catch (e: Exception) {
                System.err.println("Failed to start ClickHouse container: ${e.message}")
                // Ensure container is stopped so @Before's assumeTrue(isRunning) skips tests
                // instead of letting them fail against an empty / partially initialized server.
                try { clickhouse.stop() } catch (_: Exception) { /* best-effort cleanup */ }
            }
        }

        private fun initDatabase() {
            if (!clickhouse.isRunning) return
            val jdbcUrl = "jdbc:clickhouse://${clickhouse.host}:${clickhouse.getMappedPort(CH_HTTP_PORT)}/$CH_DATABASE"
            val initSql = ConnectorClickHouseIntegrationTest::class.java
                .classLoader
                .getResource("clickhouse/init-test-clickhouse.sql")!!
                .readText()

            var lastException: Exception? = null
            for (attempt in 1..20) {
                try {
                    DriverManager.getConnection(jdbcUrl, CH_USER, CH_PASSWORD).use { conn ->
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
            clickhouse.stop()
        }
    }

    private lateinit var connector: ConnectorClickHouse

    @Before
    fun setUp() {
        assumeTrue("Docker is not available – skipping integration test", clickhouse.isRunning)
        connector = ConnectorClickHouse(
            host = clickhouse.host,
            port = clickhouse.getMappedPort(CH_HTTP_PORT),
            database = CH_DATABASE,
            user = CH_USER,
            password = CH_PASSWORD
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
    fun `scanTables filters by database list`() = runBlocking {
        val tables = mutableListOf<FoundedFile>()
        connector.scanTables(path = CH_DATABASE) { tables.add(it) }
        assertTrue(tables.isNotEmpty(), "Expected tables in the filtered database")
        assertTrue(
            tables.all { it.path.startsWith("$CH_DATABASE.") },
            "All tables must belong to '$CH_DATABASE' database: ${tables.map { it.path }}"
        )
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
