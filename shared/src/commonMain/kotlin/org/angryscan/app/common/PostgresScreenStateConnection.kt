package org.angryscan.app.common

private const val DefaultPostgresPort = 5432

enum class PostgresConnectionRequiredField {
    HOST,
    PORT,
    DATABASE,
    USER,
    PASSWORD
}

fun ScreenStateSettings.PostgresScreenState.missingRequiredConnectionFields(): Set<PostgresConnectionRequiredField> =
    buildSet {
        if (host.isBlank()) add(PostgresConnectionRequiredField.HOST)
        if (port.toIntOrNull() == null) add(PostgresConnectionRequiredField.PORT)
        if (database.isBlank()) add(PostgresConnectionRequiredField.DATABASE)
        if (user.isBlank()) add(PostgresConnectionRequiredField.USER)
        if (password.isBlank()) add(PostgresConnectionRequiredField.PASSWORD)
    }

fun ScreenStateSettings.PostgresScreenState.updatedHighlightedConnectionFields(
    highlightedFields: Set<PostgresConnectionRequiredField>
): Set<PostgresConnectionRequiredField> =
    highlightedFields.intersect(missingRequiredConnectionFields())

fun ScreenStateSettings.PostgresScreenState.hasRequiredConnectionSettings(): Boolean =
    missingRequiredConnectionFields().isEmpty()

fun ScreenStateSettings.PostgresScreenState.connectionPort(): Int =
    port.toIntOrNull() ?: DefaultPostgresPort
