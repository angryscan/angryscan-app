package org.angryscan.app.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class SqlDatabaseScreenStateConnectionTest {

    @Test
    fun `missing required connection fields for server DB - database empty`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.PostgreSQL,
            host = "localhost",
            port = "5432",
            database = "",
            user = "postgres",
            password = "secret"
        )

        assertEquals(setOf(DatabaseConnectionRequiredField.DATABASE), state.missingRequiredConnectionFields())
        assertFalse(state.hasRequiredConnectionSettings())
    }

    @Test
    fun `invalid port is treated as missing required field`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.PostgreSQL,
            host = "localhost",
            port = "invalid",
            database = "scanner",
            user = "postgres",
            password = "secret"
        )

        assertEquals(setOf(DatabaseConnectionRequiredField.PORT), state.missingRequiredConnectionFields())
        assertFalse(state.hasRequiredConnectionSettings())
        assertEquals(5432, state.connectionPort())
    }

    @Test
    fun `connection test is enabled when all required fields are filled for server`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.PostgreSQL,
            host = "localhost",
            port = "5432",
            database = "scanner",
            user = "postgres",
            password = "secret"
        )

        assertTrue(state.missingRequiredConnectionFields().isEmpty())
        assertTrue(state.hasRequiredConnectionSettings())
        assertEquals(5432, state.connectionPort())
    }

    @Test
    fun `SQLite requires filePath`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.SQLite,
            filePath = ""
        )
        assertEquals(setOf(DatabaseConnectionRequiredField.FILE_PATH), state.missingRequiredConnectionFields())
        assertFalse(state.hasRequiredConnectionSettings())
    }

    @Test
    fun `SQLite has required settings when filePath is set`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.SQLite,
            filePath = "/path/to/db.sqlite"
        )
        assertTrue(state.missingRequiredConnectionFields().isEmpty())
        assertTrue(state.hasRequiredConnectionSettings())
    }

    @Test
    fun `empty fields are not highlighted before validation click`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.PostgreSQL,
            host = "",
            port = "5432",
            database = "",
            user = "postgres",
            password = "secret"
        )

        assertTrue(state.updatedHighlightedConnectionFields(emptySet()).isEmpty())
    }

    @Test
    fun `highlighted fields are cleared only when corrected`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.PostgreSQL,
            host = "localhost",
            port = "5432",
            database = "",
            user = "postgres",
            password = "secret"
        )

        assertEquals(
            setOf(DatabaseConnectionRequiredField.DATABASE),
            state.updatedHighlightedConnectionFields(
                setOf(
                    DatabaseConnectionRequiredField.DATABASE,
                    DatabaseConnectionRequiredField.HOST
                )
            )
        )
    }

    @Test
    fun `GreenPlum requires same server fields as PostgreSQL`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.GreenPlum,
            host = "localhost",
            port = "5432",
            database = "warehouse",
            user = "gpadmin",
            password = "secret"
        )

        assertTrue(state.missingRequiredConnectionFields().isEmpty())
        assertTrue(state.hasRequiredConnectionSettings())
        assertEquals(5432, state.connectionPort())
    }

    @Test
    fun `GreenPlum missing database is detected`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.GreenPlum,
            host = "localhost",
            port = "5432",
            database = "",
            user = "gpadmin",
            password = "secret"
        )

        assertEquals(setOf(DatabaseConnectionRequiredField.DATABASE), state.missingRequiredConnectionFields())
        assertFalse(state.hasRequiredConnectionSettings())
    }

    @Test
    fun `GreenPlum default port is 5432`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.GreenPlum,
            host = "localhost",
            port = "invalid",
            database = "warehouse",
            user = "gpadmin",
            password = "secret"
        )

        assertEquals(5432, state.connectionPort())
    }

    @Test
    fun `Hive requires same server fields as PostgreSQL`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.Hive,
            host = "localhost",
            port = "10000",
            database = "default",
            user = "hive",
            password = "hive"
        )

        assertTrue(state.missingRequiredConnectionFields().isEmpty())
        assertTrue(state.hasRequiredConnectionSettings())
        assertEquals(10000, state.connectionPort())
    }

    @Test
    fun `Hive missing user is detected`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.Hive,
            host = "localhost",
            port = "10000",
            database = "default",
            user = "",
            password = "hive"
        )

        assertEquals(setOf(DatabaseConnectionRequiredField.USER), state.missingRequiredConnectionFields())
        assertFalse(state.hasRequiredConnectionSettings())
    }

    @Test
    fun `Hive default port is 10000`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.Hive,
            host = "localhost",
            port = "invalid",
            database = "default",
            user = "hive",
            password = "hive"
        )

        assertEquals(10000, state.connectionPort())
    }

    @Test
    fun `CockroachDB requires same server fields as PostgreSQL`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.CockroachDB,
            host = "localhost",
            port = "26257",
            database = "defaultdb",
            user = "root",
            password = "secret"
        )

        assertTrue(state.missingRequiredConnectionFields().isEmpty())
        assertTrue(state.hasRequiredConnectionSettings())
        assertEquals(26257, state.connectionPort())
    }

    @Test
    fun `CockroachDB missing database is detected`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.CockroachDB,
            host = "localhost",
            port = "26257",
            database = "",
            user = "root",
            password = "secret"
        )

        assertEquals(setOf(DatabaseConnectionRequiredField.DATABASE), state.missingRequiredConnectionFields())
        assertFalse(state.hasRequiredConnectionSettings())
    }

    @Test
    fun `CockroachDB default port is 26257`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.CockroachDB,
            host = "localhost",
            port = "invalid",
            database = "defaultdb",
            user = "root",
            password = ""
        )

        assertEquals(26257, state.connectionPort())
    }

    @Test
    fun `ClickHouse requires same server fields as PostgreSQL`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.ClickHouse,
            host = "localhost",
            port = "8123",
            database = "default",
            user = "default",
            password = "secret"
        )

        assertTrue(state.missingRequiredConnectionFields().isEmpty())
        assertTrue(state.hasRequiredConnectionSettings())
        assertEquals(8123, state.connectionPort())
    }

    @Test
    fun `ClickHouse missing database is detected`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.ClickHouse,
            host = "localhost",
            port = "8123",
            database = "",
            user = "default",
            password = "secret"
        )

        assertEquals(setOf(DatabaseConnectionRequiredField.DATABASE), state.missingRequiredConnectionFields())
        assertFalse(state.hasRequiredConnectionSettings())
    }

    @Test
    fun `ClickHouse default port is 8123`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.ClickHouse,
            host = "localhost",
            port = "invalid",
            database = "default",
            user = "default",
            password = "secret"
        )

        assertEquals(8123, state.connectionPort())
    }

    @Test
    fun `Redshift requires same server fields as PostgreSQL`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.Redshift,
            host = "cluster.region.redshift.amazonaws.com",
            port = "5439",
            database = "dev",
            user = "admin",
            password = "secret"
        )

        assertTrue(state.missingRequiredConnectionFields().isEmpty())
        assertTrue(state.hasRequiredConnectionSettings())
        assertEquals(5439, state.connectionPort())
    }

    @Test
    fun `Redshift missing database is detected`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.Redshift,
            host = "cluster.region.redshift.amazonaws.com",
            port = "5439",
            database = "",
            user = "admin",
            password = "secret"
        )

        assertEquals(setOf(DatabaseConnectionRequiredField.DATABASE), state.missingRequiredConnectionFields())
        assertFalse(state.hasRequiredConnectionSettings())
    }

    @Test
    fun `Redshift default port is 5439`() {
        val state = ScreenStateSettings.SqlDatabaseScreenState(
            databaseType = DatabaseType.Redshift,
            host = "cluster.region.redshift.amazonaws.com",
            port = "invalid",
            database = "dev",
            user = "admin",
            password = "secret"
        )

        assertEquals(5439, state.connectionPort())
    }
}
