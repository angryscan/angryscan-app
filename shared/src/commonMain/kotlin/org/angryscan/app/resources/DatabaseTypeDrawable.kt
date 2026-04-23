package org.angryscan.app.resources

import org.angryscan.app.common.DatabaseType
import org.jetbrains.compose.resources.DrawableResource

/** Compose drawable for each SQL database type (chips, sidebar). */
fun DatabaseType.drawableResource(): DrawableResource = when (this) {
    DatabaseType.PostgreSQL -> Res.drawable.db_postgresql_logo
    DatabaseType.MySQL -> Res.drawable.db_mysql_logo
    DatabaseType.SQLite -> Res.drawable.db_sqlite_logo
    DatabaseType.GreenPlum -> Res.drawable.db_greenplum_logo
    DatabaseType.Hive -> Res.drawable.db_hive_logo
    DatabaseType.CockroachDB -> Res.drawable.db_cockroachdb_logo
    DatabaseType.ClickHouse -> Res.drawable.db_clickhouse_logo
    DatabaseType.Redshift -> Res.drawable.db_redshift_logo
    DatabaseType.SqlServer -> Res.drawable.db_sqlserver_logo
}
