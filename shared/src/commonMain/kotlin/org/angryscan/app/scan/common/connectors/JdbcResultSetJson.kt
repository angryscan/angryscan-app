package org.angryscan.app.scan.common.connectors

import java.sql.ResultSet

/**
 * Serializes the current row of [ResultSet] as a single-line JSON object.
 * Shared by all JDBC-based database connectors to avoid duplication.
 */
internal fun ResultSet.toJsonRow(): String {
    val metaData = metaData
    return buildString {
        append('{')
        for (index in 1..metaData.columnCount) {
            if (index > 1) append(',')
            append('"')
            append(metaData.getColumnLabel(index).escapeJson())
            append('"')
            append(':')
            append(getObject(index).toJsonValue())
        }
        append('}')
    }
}

private fun Any?.toJsonValue(): String =
    when (this) {
        null -> "null"
        is Double -> if (isFinite()) toString() else "\"${toString()}\""
        is Float -> if (isFinite()) toString() else "\"${toString()}\""
        is Number, is Boolean -> toString()
        else -> "\"${toString().escapeJson()}\""
    }

private fun String.escapeJson(): String = buildString(length) {
    for (char in this@escapeJson) {
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
}
