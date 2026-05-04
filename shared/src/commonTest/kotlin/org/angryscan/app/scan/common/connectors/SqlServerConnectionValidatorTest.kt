package org.angryscan.app.scan.common.connectors

import kotlin.test.Test
import kotlin.test.assertEquals
import java.sql.SQLException

internal class SqlServerConnectionValidatorTest {

    @Test
    fun `login failed maps to USER_PASSWORD`() {
        val e = SQLException("Login failed for user 'sa'.")
        val result = SqlServerConnectionValidator.parseConnectionError(e)
        assertEquals(DatabaseConnectionErrorField.USER_PASSWORD, result.field)
        assertEquals(
            sanitizedConnectionErrorMessage(DatabaseConnectionErrorField.USER_PASSWORD),
            result.message
        )
    }

    @Test
    fun `cannot open database maps to DATABASE`() {
        val e = SQLException("Cannot open database \"wrongdb\" requested by the login.")
        val result = SqlServerConnectionValidator.parseConnectionError(e)
        assertEquals(DatabaseConnectionErrorField.DATABASE, result.field)
        assertEquals(
            sanitizedConnectionErrorMessage(DatabaseConnectionErrorField.DATABASE),
            result.message
        )
    }

    @Test
    fun `connection refused maps to HOST`() {
        val e = SQLException("Connection refused: connect")
        val result = SqlServerConnectionValidator.parseConnectionError(e)
        assertEquals(DatabaseConnectionErrorField.HOST, result.field)
        assertEquals(
            sanitizedConnectionErrorMessage(DatabaseConnectionErrorField.HOST),
            result.message
        )
    }
}
