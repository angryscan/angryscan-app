package org.angryscan.app.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

object LogLevel {

    /**
     * Sets Logback root level when Logback is the active SLF4J binding.
     * If another binding or NOP is on the classpath (e.g. wrong jar order with hive-jdbc), this is a no-op.
     */
    fun setLoggingLevel(level: Level) {
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        applyLevelIfLogback(root, level)
    }

    internal fun applyLevelOrThrow(rootLogger: Any, level: Level): Boolean {
        val logbackLogger = rootLogger as? Logger ?: throw IllegalStateException(
            "Expected Logback logger, but got ${rootLogger::class.qualifiedName}. " +
                "Check SLF4J bindings on classpath."
        )
        logbackLogger.level = level
        return true
    }

    private fun applyLevelIfLogback(rootLogger: Any, level: Level) {
        val logbackLogger = rootLogger as? Logger ?: return
        logbackLogger.level = level
    }
}