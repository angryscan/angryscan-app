package org.angryscan.app.ui.extensions

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.char

val fileDateFormat = LocalDateTime.Format {
    year()
    char('-')
    monthNumber()
    char('-')
    day()
    char('_')
    hour()
    char('-')
    minute()
    char('-')
    second()
}