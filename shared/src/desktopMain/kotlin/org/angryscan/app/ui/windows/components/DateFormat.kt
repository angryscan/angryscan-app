package org.angryscan.app.ui.windows.components

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.char

val DateFormat = LocalDateTime.Format {
    dayOfMonth()
    char('.')
    monthNumber()
    char('.')
    year()
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
}