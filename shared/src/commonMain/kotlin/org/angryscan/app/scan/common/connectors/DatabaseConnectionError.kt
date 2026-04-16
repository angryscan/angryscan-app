package org.angryscan.app.scan.common.connectors

/**
 * Represents a database connection validation error with a hint which field to fix.
 */
data class DatabaseConnectionError(
    val field: DatabaseConnectionErrorField,
    val message: String
)

enum class DatabaseConnectionErrorField {
    HOST,
    PORT,
    DATABASE,
    USER,
    PASSWORD,
    /** Authentication failed; highlight both User and Password fields. */
    USER_PASSWORD,
    /** SQLite: file path is invalid or file does not exist. */
    FILE_PATH
}
