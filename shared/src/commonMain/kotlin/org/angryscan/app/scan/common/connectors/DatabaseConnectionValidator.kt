package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.angryscan.app.common.DatabaseType

/**
 * Facade for database connection validation. Delegates to type-specific validators.
 */
object DatabaseConnectionValidator {

    suspend fun validate(
        databaseType: DatabaseType,
        host: String = "",
        port: Int = 0,
        database: String = "",
        user: String = "",
        password: String = "",
        filePath: String = "",
        authDatabase: String = ""
    ): DatabaseConnectionError? = withContext(Dispatchers.IO) {
        when (databaseType) {
            DatabaseType.PostgreSQL ->
                PostgresConnectionValidator.validate(host, port, database, user, password)
            DatabaseType.MySQL ->
                MySqlConnectionValidator.validate(host, port, database, user, password)
            DatabaseType.GreenPlum ->
                GreenPlumConnectionValidator.validate(host, port, database, user, password)
            DatabaseType.Hive ->
                HiveConnectionValidator.validate(host, port, database, user, password)
            DatabaseType.CockroachDB ->
                CockroachDBConnectionValidator.validate(host, port, database, user, password)
            DatabaseType.ClickHouse ->
                ClickHouseConnectionValidator.validate(host, port, database, user, password)
            DatabaseType.Redshift ->
                RedshiftConnectionValidator.validate(host, port, database, user, password)
            DatabaseType.SqlServer ->
                SqlServerConnectionValidator.validate(host, port, database, user, password)
            DatabaseType.MongoDB ->
                MongoConnectionValidator.validate(host, port, database, user, password, authDatabase)
            DatabaseType.SQLite ->
                SqliteConnectionValidator.validate(filePath)
        }
    }
}
