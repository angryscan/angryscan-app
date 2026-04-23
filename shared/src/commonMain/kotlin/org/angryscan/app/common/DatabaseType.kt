package org.angryscan.app.common

import kotlinx.serialization.Serializable

/**
 * Supported SQL database types for scanning.
 */
@Serializable
enum class DatabaseType {
    PostgreSQL,
    MySQL,
    SQLite,
    GreenPlum,
    Hive,
    CockroachDB,
    ClickHouse,
    Redshift,
    SqlServer
}

/** Short label for type picker (sidebar, chips). */
fun DatabaseType.typePickerLabel(): String = when (this) {
    DatabaseType.Redshift -> "Amazon Redshift"
    DatabaseType.SqlServer -> "Microsoft SQL Server"
    else -> name
}
