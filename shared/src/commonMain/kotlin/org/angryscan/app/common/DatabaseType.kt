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
    SqlServer,

    /**
     * Legacy value kept only for deserializing old [ScreenStateSettings] / saved connection JSON.
     * It is migrated to [PostgreSQL] on load; do not show in UI pickers.
     */
    MongoDB,
}

/** Database types shown in UI chips and sidebar (excludes legacy-only values). */
fun databaseTypesForPicker(): List<DatabaseType> =
    DatabaseType.entries.filter { it != DatabaseType.MongoDB }

/** Short label for type picker (sidebar, chips). */
fun DatabaseType.typePickerLabel(): String = when (this) {
    DatabaseType.Redshift -> "Amazon Redshift"
    DatabaseType.SqlServer -> "Microsoft SQL Server"
    DatabaseType.MongoDB -> "MongoDB (legacy)"
    else -> name
}
