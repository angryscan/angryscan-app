package org.angryscan.app.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class PostgresScreenStateConnectionTest {

    @Test
    fun `missing required connection fields are returned`() {
        val state = ScreenStateSettings.PostgresScreenState(
            host = "localhost",
            port = "5432",
            database = "",
            user = "postgres",
            password = "secret"
        )

        assertEquals(setOf(PostgresConnectionRequiredField.DATABASE), state.missingRequiredConnectionFields())
        assertFalse(state.hasRequiredConnectionSettings())
    }

    @Test
    fun `invalid port is treated as missing required field`() {
        val state = ScreenStateSettings.PostgresScreenState(
            host = "localhost",
            port = "invalid",
            database = "scanner",
            user = "postgres",
            password = "secret"
        )

        assertEquals(setOf(PostgresConnectionRequiredField.PORT), state.missingRequiredConnectionFields())
        assertFalse(state.hasRequiredConnectionSettings())
        assertEquals(5432, state.connectionPort())
    }

    @Test
    fun `connection test is enabled when all required fields are filled`() {
        val state = ScreenStateSettings.PostgresScreenState(
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
    fun `empty fields are not highlighted before validation click`() {
        val state = ScreenStateSettings.PostgresScreenState(
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
        val state = ScreenStateSettings.PostgresScreenState(
            host = "localhost",
            port = "5432",
            database = "",
            user = "postgres",
            password = "secret"
        )

        assertEquals(
            setOf(PostgresConnectionRequiredField.DATABASE),
            state.updatedHighlightedConnectionFields(
                setOf(
                    PostgresConnectionRequiredField.DATABASE,
                    PostgresConnectionRequiredField.HOST
                )
            )
        )
    }
}
