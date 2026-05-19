package org.angryscan.app.common

import kotlinx.serialization.Serializable

/**
 * Supported database types for scanning (relational SQL engines and MongoDB).
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
    SqlServer,
    MongoDB,
}

/** Short label for type picker (sidebar, chips). */
fun DatabaseType.typePickerLabel(): String = when (this) {
    DatabaseType.Redshift -> "Amazon Redshift"
    DatabaseType.SqlServer -> "Microsoft SQL Server"
    else -> name
}
