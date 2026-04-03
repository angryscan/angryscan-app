package org.angryscan.app.logging

import ch.qos.logback.classic.Level
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LogLevelTest {

    @Test
    fun applyLevelThrowsForNonLogbackLogger() {
        assertFailsWith<IllegalStateException> {
            LogLevel.applyLevelOrThrow(Any(), Level.INFO)
        }
    }

    @Test
    fun applyLevelThrowsMessageMentionsActualLoggerType() {
        val error = assertFailsWith<IllegalStateException> {
            LogLevel.applyLevelOrThrow(Any(), Level.INFO)
        }

        assertTrue(error.message?.contains("Expected Logback logger") == true)
        assertTrue(error.message?.contains("Check SLF4J bindings on classpath") == true)
    }

    @Test
    fun applyLevelReturnsTrueForLogbackLogger() {
        val logger = ch.qos.logback.classic.LoggerContext().getLogger("test")

        val applied = LogLevel.applyLevelOrThrow(logger, Level.DEBUG)

        assertTrue(applied)
    }
}
