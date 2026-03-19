package org.angryscan.app.common

private const val DefaultPostgresPort = 5432
private const val DefaultMySqlPort = 3306

/**
 * Required connection fields for database types.
 * - Server DBs (PostgreSQL, MySQL): HOST, PORT, DATABASE, USER, PASSWORD
 * - SQLite: FILE_PATH only
 */
enum class DatabaseConnectionRequiredField {
    HOST,
    PORT,
    DATABASE,
    USER,
    PASSWORD,
    /** SQLite: path to .db file */
    FILE_PATH
}

fun ScreenStateSettings.SqlDatabaseScreenState.missingRequiredConnectionFields(): Set<DatabaseConnectionRequiredField> =
    when (databaseType) {
        DatabaseType.PostgreSQL, DatabaseType.MySQL -> buildSet {
            if (host.isBlank()) add(DatabaseConnectionRequiredField.HOST)
            if (port.toIntOrNull() == null) add(DatabaseConnectionRequiredField.PORT)
            if (database.isBlank()) add(DatabaseConnectionRequiredField.DATABASE)
            if (user.isBlank()) add(DatabaseConnectionRequiredField.USER)
            if (password.isBlank()) add(DatabaseConnectionRequiredField.PASSWORD)
        }
        DatabaseType.SQLite -> buildSet {
            if (filePath.isBlank()) add(DatabaseConnectionRequiredField.FILE_PATH)
        }
    }

fun ScreenStateSettings.SqlDatabaseScreenState.updatedHighlightedConnectionFields(
    highlightedFields: Set<DatabaseConnectionRequiredField>
): Set<DatabaseConnectionRequiredField> =
    highlightedFields.intersect(missingRequiredConnectionFields())

fun ScreenStateSettings.SqlDatabaseScreenState.hasRequiredConnectionSettings(): Boolean =
    missingRequiredConnectionFields().isEmpty()

fun ScreenStateSettings.SqlDatabaseScreenState.connectionPort(): Int =
    when (databaseType) {
        DatabaseType.PostgreSQL -> port.toIntOrNull() ?: DefaultPostgresPort
        DatabaseType.MySQL -> port.toIntOrNull() ?: DefaultMySqlPort
        DatabaseType.SQLite -> 0
    }
