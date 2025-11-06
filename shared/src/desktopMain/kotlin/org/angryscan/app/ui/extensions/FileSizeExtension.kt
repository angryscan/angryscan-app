package org.angryscan.app.ui.extensions

fun Long.toHumanReadable(): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = this.toDouble()

    for (unit in units) {
        if (size < 1024 || unit == units.last()) {
            return "${String.format("%.2f", size)} $unit"
        }
        size /= 1024
    }

    return "$this B"
}