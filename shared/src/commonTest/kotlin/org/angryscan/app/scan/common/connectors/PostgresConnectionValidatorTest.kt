package org.angryscan.app.scan.common.connectors

import kotlin.test.Test
import kotlin.test.assertEquals
import java.sql.SQLException

internal class PostgresConnectionValidatorTest {

    @Test
    fun `password authentication failed maps to USER_PASSWORD`() {
        val e = SQLException("FATAL: password authentication failed for user \"postgres\"")
        val result = PostgresConnectionValidator.parseConnectionError(e)
        assertEquals(PostgresConnectionErrorField.USER_PASSWORD, result.field)
    }

    @Test
    fun `connection refused maps to HOST`() {
        val e = SQLException("Connection refused. Check that the hostname and port are correct.")
        val result = PostgresConnectionValidator.parseConnectionError(e)
        assertEquals(PostgresConnectionErrorField.HOST, result.field)
    }

    @Test
    fun `database does not exist maps to DATABASE`() {
        val e = SQLException("FATAL: database \"wrongdb\" does not exist")
        val result = PostgresConnectionValidator.parseConnectionError(e)
        assertEquals(PostgresConnectionErrorField.DATABASE, result.field)
    }

    @Test
    fun `role does not exist maps to USER`() {
        val e = SQLException("FATAL: role \"wronguser\" does not exist")
        val result = PostgresConnectionValidator.parseConnectionError(e)
        assertEquals(PostgresConnectionErrorField.USER, result.field)
    }
}
