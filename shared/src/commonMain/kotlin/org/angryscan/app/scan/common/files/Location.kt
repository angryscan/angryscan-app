package org.angryscan.app.scan.common.files

import org.angryscan.common.engine.Match

data class Location(
    val entry: Match,
    val location: String
)