package org.angryscan.app.scan.common

import org.angryscan.common.engine.IMatcher

/** Common type for file scan (Document) and per-column DB scan (DocumentWithColumns). */
interface IScanResult {
    fun getDocumentFields(): Map<IMatcher, Int>
    fun isEmpty(): Boolean
    fun skipped(): Boolean
}