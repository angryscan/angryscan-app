package org.angryscan.app.scan.common

import org.angryscan.common.engine.IMatcher

/**
 * Result of per-column DB scan. Holds matcher counts per column name.
 * Aggregate counts (all columns merged) are available for compatibility with file-scan flow.
 */
class DocumentWithColumns(
    val path: String,
    val size: Long,
    val columnFields: Map<String, Map<IMatcher, Int>>
) : IScanResult {
    private var skipped = false

    fun skip(): DocumentWithColumns {
        skipped = true
        return this
    }

    override fun skipped(): Boolean = skipped

    /** Aggregate of all column fields (matcher -> total count across columns). */
    override fun getDocumentFields(): Map<IMatcher, Int> =
        columnFields.values.fold(mutableMapOf<IMatcher, Int>()) { acc, map ->
            map.forEach { (matcher, count) ->
                acc[matcher] = (acc[matcher] ?: 0) + count
            }
            acc
        }

    override fun isEmpty(): Boolean = columnFields.isEmpty() || columnFields.values.all { it.isEmpty() }
}
