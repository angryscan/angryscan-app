package org.angryscan.app.scan.common.files

import org.angryscan.common.engine.IMask
import org.angryscan.common.engine.Match
import kotlin.reflect.full.isSubclassOf

interface Location{
    val entry: Match
    val location: String
    val attachmentName: String?
    val isMaskable
        get() = attachmentName == null && entry.matcher::class.isSubclassOf(IMask::class)
}