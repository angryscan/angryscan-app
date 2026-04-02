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
 * Integration test against a GreenPlum instance managed by Testcontainers.
 * Requires Docker to be running. The container is started once for all tests
 * and initialized with test data from greenplum/init-test-greenplum.sql.
 *
 * Uses datagrip/greenplum:6.8 image which provides a single-node GP cluster.
 * Default credentials: gpadmin / pivotal, database: postgres, port: 5432.
 */
internal class ConnectorGreenPlumIntegrationTest {

    companion object {
        private const val GP_PORT = 5432
        private const val GP_USER = "gpadmin"
        private const val GP_PASSWORD = "pivotal"
        private const val GP_DATABASE = "postgres"

        @Suppress("DEPRECATION")
        private val greenplum: GenericContainer<*> = GenericContainer("datagrip/greenplum:6.8")
            .withExposedPorts(GP_PORT)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(180)))

        @BeforeClass
        @JvmStatic
        fun startContainer() {
            try {
                greenplum.start()
                allowRemoteConnections()
                initDatabase()
            } catch (e: Exception) {
                System.err.println("Failed to start GreenPlum container: ${e.message}")
            }
        }

        private fun allowRemoteConnections() {
            if (!greenplum.isRunning) return
            greenplum.execInContainer(
                "bash", "-c",
                """find / -name pg_hba.conf 2>/dev/null | while read f; do echo 'host all all 0.0.0.0/0 trust' >> "${'$'}f"; done"""
            )
            greenplum.execInContainer(
                "su", "-", "gpadmin", "-c", "psql -c 'SELECT pg_reload_conf()'"
            )
        }

        private fun initDatabase() {
            if (!greenplum.isRunning) return
            val jdbcUrl = "jdbc:postgresql://${greenplum.host}:${greenplum.getMappedPort(GP_PORT)}/$GP_DATABASE"
            val initSql = ConnectorGreenPlumIntegrationTest::class.java
                .classLoader
                .getResource("greenplum/init-test-greenplum.sql")!!
                .readText()

            var lastException: Exception? = null
            for (attempt in 1..20) {
                try {
                    DriverManager.getConnection(jdbcUrl, GP_USER, GP_PASSWORD).use { conn ->
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
            greenplum.stop()
        }
    }

    private lateinit var connector: ConnectorGreenPlum

    @Before
    fun setUp() {
        assumeTrue("Docker is not available – skipping integration test", greenplum.isRunning)
        connector = ConnectorGreenPlum(
            host = greenplum.host,
            port = greenplum.getMappedPort(GP_PORT),
            database = GP_DATABASE,
            user = GP_USER,
            password = GP_PASSWORD
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
