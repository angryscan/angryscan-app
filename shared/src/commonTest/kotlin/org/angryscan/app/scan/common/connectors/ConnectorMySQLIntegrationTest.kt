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
import org.testcontainers.containers.MySQLContainer
import kotlin.test.assertTrue

/**
 * Integration test against a MySQL instance managed by Testcontainers.
 * Requires Docker to be running. The container is started once for all tests
 * and initialized with test data from mysql/init-test-mysql.sql.
 */
internal class ConnectorMySQLIntegrationTest {

    companion object {
        private val mysql = MySQLContainer("mysql:8.0.41")
            .withDatabaseName("test")
            .withUsername("mysql")
            .withPassword("mysql")
            .withInitScript("mysql/init-test-mysql.sql")

        @BeforeClass
        @JvmStatic
        fun startContainer() {
            try {
                mysql.start()
            } catch (e: Exception) {
                System.err.println("Failed to start MySQL container: ${e.message}")
            }
        }

        @AfterClass
        @JvmStatic
        fun stopContainer() {
            mysql.stop()
        }
    }

    private lateinit var connector: ConnectorMySQL

    @Before
    fun setUp() {
        assumeTrue("Docker is not available – skipping integration test", mysql.isRunning())
        connector = ConnectorMySQL(
            host = mysql.getHost(),
            port = mysql.getFirstMappedPort(),
            database = mysql.getDatabaseName(),
            user = mysql.getUsername(),
            password = mysql.getPassword()
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
    fun `large tables are capped by rowLimit`() = runBlocking {
        val bulkConnector = ConnectorMySQL(
            host = mysql.getHost(),
            port = mysql.getFirstMappedPort(),
            database = mysql.getDatabaseName(),
            user = mysql.getUsername(),
            password = mysql.getPassword(),
            rowLimit = 100
        )
        val tables = mutableListOf<FoundedFile>()
        bulkConnector.scanTables(path = "") { tables.add(it) }

        val bulkTable = tables.firstOrNull { "bulk_emails" in it.path }
        assumeTrue("bulk_emails table not found", bulkTable != null)

        val rows = bulkConnector.getTableContentStructured(bulkTable!!.path)
        assertTrue(rows.size <= 100, "Expected at most 100 rows, got ${rows.size}")
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
