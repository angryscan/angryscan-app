package org.angryscan.app.scan

import org.angryscan.app.scan.common.FileSize
import org.angryscan.common.engine.IMatcher

data class SqlColumnResultCard(
    val name: String,
    val foundAttributes: Map<IMatcher, Int>,
    val count: Int,
    val score: Long
)

data class SqlTableResultCard(
    val schemaName: String?,
    val tableName: String,
    val tablePath: String,
    val size: FileSize,
    val foundAttributes: Map<IMatcher, Int>,
    val count: Int,
    val score: Long,
    val columns: List<SqlColumnResultCard>
)

fun groupSqlResultsByTable(results: List<TaskFileResult>): List<SqlTableResultCard> =
    results
        .filter { !it.columnName.isNullOrBlank() }
        .groupBy(TaskFileResult::path)
        .map { (tablePath, tableRows) ->
            val parsedPath = parseSqlTablePath(tablePath)
            val columns = tableRows
                .groupBy { it.columnName.orEmpty() }
                .map { (columnName, columnRows) ->
                    SqlColumnResultCard(
                        name = columnName,
                        foundAttributes = aggregateAttributes(columnRows.map(TaskFileResult::foundAttributes)),
                        count = columnRows.sumOf(TaskFileResult::count),
                        score = columnRows.sumOf(TaskFileResult::score)
                    )
                }
                .sortedWith(compareByDescending<SqlColumnResultCard> { it.score }.thenBy { it.name })

            SqlTableResultCard(
                schemaName = parsedPath.schemaName,
                tableName = parsedPath.tableName,
                tablePath = tablePath,
                size = tableRows.first().size,
                foundAttributes = aggregateAttributes(columns.map(SqlColumnResultCard::foundAttributes)),
                count = columns.sumOf(SqlColumnResultCard::count),
                score = columns.sumOf(SqlColumnResultCard::score),
                columns = columns
            )
        }
        .sortedWith(compareByDescending<SqlTableResultCard> { it.score }.thenBy { it.tablePath })

private data class ParsedSqlTablePath(
    val schemaName: String?,
    val tableName: String
)

private fun parseSqlTablePath(tablePath: String): ParsedSqlTablePath {
    val separatorIndex = tablePath.indexOf('.')

    if (separatorIndex <= 0 || separatorIndex >= tablePath.lastIndex) {
        return ParsedSqlTablePath(
            schemaName = null,
            tableName = tablePath
        )
    }

    return ParsedSqlTablePath(
        schemaName = tablePath.substring(0, separatorIndex),
        tableName = tablePath.substring(separatorIndex + 1)
    )
}

private fun aggregateAttributes(attributeMaps: List<Map<IMatcher, Int>>): Map<IMatcher, Int> =
    buildMap {
        attributeMaps.forEach { attributes ->
            attributes.forEach { (matcher, count) ->
                put(matcher, (get(matcher) ?: 0) + count)
            }
        }
    }
