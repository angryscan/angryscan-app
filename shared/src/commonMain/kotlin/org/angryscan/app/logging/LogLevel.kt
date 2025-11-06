package org.angryscan.app.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

object LogLevel {

    fun setLoggingLevel(level: Level) {
        val root =
            LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.setLevel(level)
    }
}