package org.angryscan.app.scan.common.files.locations

import org.angryscan.app.scan.common.files.Location
import org.angryscan.common.engine.Match

data class TextLocation(
    override val entry: Match,
    val line: Int,
    val position: Long
): Location {
    override val location: String
        get() = "Ln $line, Pos $position"

    override val attachmentName: String? = null
}
