package org.angryscan.app.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

object LogLevel {

    fun setLoggingLevel(level: Level) {
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        applyLevelOrThrow(root, level)
    }

    internal fun applyLevelOrThrow(rootLogger: Any, level: Level): Boolean {
        val logbackLogger = rootLogger as? Logger ?: throw IllegalStateException(
            "Expected Logback logger, but got ${rootLogger::class.qualifiedName}. " +
                "Check SLF4J bindings on classpath."
        )
        logbackLogger.level = level
        return true
    }
}