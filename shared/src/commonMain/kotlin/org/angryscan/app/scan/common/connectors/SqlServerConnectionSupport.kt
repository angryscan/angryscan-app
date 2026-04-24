package org.angryscan.app.scan.common.connectors

import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

/**
 * Shared JDBC URL and properties for SQL Server (connector and validator).
 * Database name is passed as a property so special characters do not break the URL.
 * TLS: encrypt traffic; trust server certificate for typical corporate/self-signed setups.
 */
internal object SqlServerConnectionSupport {

    private const val LOGIN_TIMEOUT_SEC = "10"
    private const val SOCKET_TIMEOUT_MS = "30000"

    fun jdbcUrl(host: String, port: Int): String =
        "jdbc:sqlserver://${host.trim()}:$port"

    fun connectionProperties(database: String, user: String, password: String): Properties =
        Properties().apply {
            setProperty("databaseName", database)
            setProperty("user", user)
            setProperty("password", password)
            setProperty("encrypt", "true")
            setProperty("trustServerCertificate", "true")
            setProperty("loginTimeout", LOGIN_TIMEOUT_SEC)
            setProperty("socketTimeout", SOCKET_TIMEOUT_MS)
        }

    fun openConnection(host: String, port: Int, database: String, user: String, password: String): Connection =
        DriverManager.getConnection(
            jdbcUrl(host, port),
            connectionProperties(database, user, password)
        )
}
