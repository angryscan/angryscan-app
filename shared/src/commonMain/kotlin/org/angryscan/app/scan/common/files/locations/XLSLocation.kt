package org.angryscan.app.scan.common.files.locations

import org.angryscan.app.scan.common.files.Location
import org.angryscan.common.engine.Match

data class XLSLocation(
    override val entry: Match,
    val sheet: String,
    val col: Int,
    val row: Int,
    val cell: String,
    override val attachmentName: String? = null
) : Location {

    override val location: String
        get() = "Sheet: $sheet, Cell: $cell"

}
