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

/**
 * User-visible error text without vendor/driver exception details (avoids leaking host, paths, or logins).
 */
internal fun sanitizedConnectionErrorMessage(field: DatabaseConnectionErrorField): String =
    when (field) {
        DatabaseConnectionErrorField.HOST ->
            "Could not connect to the server. Check host, port, and network access."
        DatabaseConnectionErrorField.PORT ->
            "Invalid or unreachable port."
        DatabaseConnectionErrorField.DATABASE ->
            "The database could not be opened or does not exist."
        DatabaseConnectionErrorField.USER ->
            "The database user is not valid or does not exist."
        DatabaseConnectionErrorField.PASSWORD ->
            "The password is not valid."
        DatabaseConnectionErrorField.USER_PASSWORD ->
            "Authentication failed. Check user name and password."
        DatabaseConnectionErrorField.FILE_PATH ->
            "The database file path is invalid or the file could not be opened."
    }
