package org.angryscan.app.scan.common.files.locations

import org.angryscan.app.scan.common.files.Location
import org.angryscan.common.engine.Match

data class BaseLocation(
    override val entry: Match,
    override val location: String,
    override val attachmentName: String? = null
) : Location
