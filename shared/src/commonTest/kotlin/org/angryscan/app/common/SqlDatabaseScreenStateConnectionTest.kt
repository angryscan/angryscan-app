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
}
