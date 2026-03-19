package org.angryscan.app.common

import kotlinx.serialization.Serializable

/**
 * Supported SQL database types for scanning.
 */
@Serializable
enum class DatabaseType {
    PostgreSQL,
    MySQL,
    SQLite
}
