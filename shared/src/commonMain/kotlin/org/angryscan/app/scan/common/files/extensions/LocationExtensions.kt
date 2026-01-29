package org.angryscan.app.scan.common.files.extensions

import org.angryscan.app.scan.common.files.Location
import org.angryscan.common.engine.IMask

fun Location.mask(): String {
    val masker = this.entry.matcher as IMask
    return masker.mask(this.entry.value)
}

fun Location.isMaskable(): Boolean {
    return this.attachmentName == null && this.entry.matcher is IMask
}