package org.angryscan.app.scan.common.connectors

/**
 * Represents a PostgreSQL connection validation error with a hint which field to fix.
 */
data class PostgresConnectionError(
    val field: PostgresConnectionErrorField,
    val message: String
)

enum class PostgresConnectionErrorField {
    HOST,
    PORT,
    DATABASE,
    USER,
    PASSWORD,
    /** Authentication failed; highlight both User and Password fields. */
    USER_PASSWORD
}
