package org.angryscan.app.searcher

import org.angryscan.app.scan.common.connectors.*
import org.angryscan.app.serializers.PolymorphicFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class ConnectorSerializationTest {
    @Test
    fun `ConnectorPostgres is serialized polymorphically`() {
        val connector: IConnector = ConnectorPostgres(
            host = "localhost",
            port = 5432,
            database = "scanner",
            user = "postgres",
            password = "secret",
            rowLimit = 1000
        )

        val serialized = PolymorphicFormatter.encodeToString(connector)
        val decoded: IConnector = PolymorphicFormatter.decodeFromString(serialized)
        val postgres = assertIs<ConnectorPostgres>(decoded)

        assertEquals("localhost", postgres.host)
        assertEquals(5432, postgres.port)
        assertEquals("scanner", postgres.database)
        assertEquals("postgres", postgres.user)
        assertEquals("secret", postgres.password)
        assertEquals(1000, postgres.rowLimit)
    }

    @Test
    fun `connectors expose correct runtime contracts`() {
        assertIs<IFileConnector>(ConnectorFileShare())
        assertIs<IFileConnector>(ConnectorS3("access", "secret", "http://localhost:9000", "bucket"))
        assertIs<IFileConnector>(ConnectorHTTP())
        assertIs<IFileConnector>(ConnectorAIModels())

        val postgres = ConnectorPostgres(
            host = "localhost",
            port = 5432,
            database = "scanner",
            user = "postgres",
            password = "secret",
            rowLimit = 1000
        )

        assertIs<IDatabaseConnector>(postgres)
    }
}
