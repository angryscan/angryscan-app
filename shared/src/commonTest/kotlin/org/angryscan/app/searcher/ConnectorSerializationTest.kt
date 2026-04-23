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
    fun `ConnectorMySQL is serialized polymorphically`() {
        val connector: IConnector = ConnectorMySQL(
            host = "localhost",
            port = 3306,
            database = "scanner",
            user = "root",
            password = "secret",
            rowLimit = 1000
        )

        val serialized = PolymorphicFormatter.encodeToString(connector)
        val decoded: IConnector = PolymorphicFormatter.decodeFromString(serialized)
        val mysql = assertIs<ConnectorMySQL>(decoded)

        assertEquals("localhost", mysql.host)
        assertEquals(3306, mysql.port)
        assertEquals("scanner", mysql.database)
        assertEquals("root", mysql.user)
        assertEquals("secret", mysql.password)
        assertEquals(1000, mysql.rowLimit)
    }

    @Test
    fun `ConnectorSqlite is serialized polymorphically`() {
        val connector: IConnector = ConnectorSqlite(
            filePath = "/path/to/db.sqlite",
            rowLimit = 500
        )

        val serialized = PolymorphicFormatter.encodeToString(connector)
        val decoded: IConnector = PolymorphicFormatter.decodeFromString(serialized)
        val sqlite = assertIs<ConnectorSqlite>(decoded)

        assertEquals("/path/to/db.sqlite", sqlite.filePath)
        assertEquals(500, sqlite.rowLimit)
    }

    @Test
    fun `ConnectorGreenPlum is serialized polymorphically`() {
        val connector: IConnector = ConnectorGreenPlum(
            host = "localhost",
            port = 5432,
            database = "scanner",
            user = "gpadmin",
            password = "secret",
            rowLimit = 1000
        )

        val serialized = PolymorphicFormatter.encodeToString(connector)
        val decoded: IConnector = PolymorphicFormatter.decodeFromString(serialized)
        val greenplum = assertIs<ConnectorGreenPlum>(decoded)

        assertEquals("localhost", greenplum.host)
        assertEquals(5432, greenplum.port)
        assertEquals("scanner", greenplum.database)
        assertEquals("gpadmin", greenplum.user)
        assertEquals("secret", greenplum.password)
        assertEquals(1000, greenplum.rowLimit)
    }

    @Test
    fun `ConnectorRedshift is serialized polymorphically`() {
        val connector: IConnector = ConnectorRedshift(
            host = "cluster.region.redshift.amazonaws.com",
            port = 5439,
            database = "dev",
            user = "admin",
            password = "secret",
            rowLimit = 1000
        )

        val serialized = PolymorphicFormatter.encodeToString(connector)
        val decoded: IConnector = PolymorphicFormatter.decodeFromString(serialized)
        val redshift = assertIs<ConnectorRedshift>(decoded)

        assertEquals("cluster.region.redshift.amazonaws.com", redshift.host)
        assertEquals(5439, redshift.port)
        assertEquals("dev", redshift.database)
        assertEquals("admin", redshift.user)
        assertEquals("secret", redshift.password)
        assertEquals(1000, redshift.rowLimit)
    }

    @Test
    fun `ConnectorHive is serialized polymorphically`() {
        val connector: IConnector = ConnectorHive(
            host = "localhost",
            port = 10000,
            database = "default",
            user = "hive",
            password = "hive",
            rowLimit = 500
        )

        val serialized = PolymorphicFormatter.encodeToString(connector)
        val decoded: IConnector = PolymorphicFormatter.decodeFromString(serialized)
        val hive = assertIs<ConnectorHive>(decoded)

        assertEquals("localhost", hive.host)
        assertEquals(10000, hive.port)
        assertEquals("default", hive.database)
        assertEquals("hive", hive.user)
        assertEquals("hive", hive.password)
        assertEquals(500, hive.rowLimit)
    }

    @Test
    fun `connectors expose correct runtime contracts`() {
        assertIs<IFileConnector>(ConnectorFileShare())
        assertIs<IFileConnector>(ConnectorS3("access", "secret", "http://localhost:9000", "bucket"))
        assertIs<IFileConnector>(ConnectorHTTP())
        assertIs<IFileConnector>(ConnectorAIModels())

        assertIs<IDatabaseConnector>(ConnectorPostgres(
            host = "localhost",
            port = 5432,
            database = "scanner",
            user = "postgres",
            password = "secret",
            rowLimit = 1000
        ))
        assertIs<IDatabaseConnector>(ConnectorMySQL(
            host = "localhost",
            port = 3306,
            database = "scanner",
            user = "root",
            password = "secret",
            rowLimit = 1000
        ))
        assertIs<IDatabaseConnector>(ConnectorSqlite(
            filePath = "/path/to/db.sqlite",
            rowLimit = 1000
        ))
        assertIs<IDatabaseConnector>(ConnectorGreenPlum(
            host = "localhost",
            port = 5432,
            database = "scanner",
            user = "gpadmin",
            password = "secret",
            rowLimit = 1000
        ))
        assertIs<IDatabaseConnector>(ConnectorHive(
            host = "localhost",
            port = 10000,
            database = "default",
            user = "hive",
            password = "hive",
            rowLimit = 1000
        ))
        assertIs<IDatabaseConnector>(ConnectorRedshift(
            host = "cluster.region.redshift.amazonaws.com",
            port = 5439,
            database = "dev",
            user = "admin",
            password = "secret",
            rowLimit = 1000
        ))
    }
}
