package org.angryscan.app.scan

import org.angryscan.app.scan.common.Document
import org.angryscan.app.scan.common.DocumentWithColumns
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine

object DatabaseContentScanner {
    fun scanContent(
        path: String,
        content: String,
        engines: List<IScanEngine>
    ): Document {
        val document = Document(content.length.toLong(), path)

        engines.forEach { engine ->
            engine.scan(content)
                .groupBy { it.matcher }
                .forEach { (matcher, matches) ->
                    document.updateDocument(matcher, matches.size)
                }
        }

        return document
    }

    /**
     * Scans each column separately and returns per-column matcher counts.
     * Used for DB scan so "where found" is the column name.
     */
    fun scanContentStructured(
        path: String,
        rows: List<Map<String, String>>,
        engines: List<IScanEngine>
    ): DocumentWithColumns {
        if (rows.isEmpty()) {
            return DocumentWithColumns(path = path, size = 0L, columnFields = emptyMap())
        }
        val columnNames = rows.first().keys
        val totalSize = rows.sumOf { row -> row.values.sumOf { it.length.toLong() } }
        val columnFields = mutableMapOf<String, MutableMap<IMatcher, Int>>()

        columnNames.forEach { columnName ->
            val columnText = rows
                .mapNotNull { it[columnName] }
                .filterNot { it.isEmpty() }
                .joinToString("\n")
            val matcherCounts = mutableMapOf<IMatcher, Int>()
            engines.forEach { engine ->
                engine.scan(columnText)
                    .groupBy { it.matcher }
                    .forEach { (matcher, matches) ->
                        matcherCounts[matcher] = (matcherCounts[matcher] ?: 0) + matches.size
                    }
            }
            if (matcherCounts.isNotEmpty()) {
                columnFields[columnName] = matcherCounts
            }
        }

        return DocumentWithColumns(path = path, size = totalSize, columnFields = columnFields)
    }
}
