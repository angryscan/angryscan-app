package org.angryscan.app.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggerContextListener
import ch.qos.logback.core.spi.ContextAwareBase
import ch.qos.logback.core.spi.LifeCycle
import org.angryscan.app.common.AppFiles
import kotlin.io.path.absolutePathString

class LoggerStartupListener : ContextAwareBase(), LoggerContextListener, LifeCycle {
    private var started = false

    override fun start() {
        if (started) return

        val logDir = AppFiles.LoggingDir

        val context = getContext()

        context.putProperty("LOG_DIR", logDir.absolutePathString())

        started = true
    }

    override fun stop() {
    }

    override fun isStarted(): Boolean {
        return started
    }

    override fun isResetResistant(): Boolean {
        return true
    }

    override fun onStart(context: LoggerContext) {
    }

    override fun onReset(context: LoggerContext) {
    }

    override fun onStop(context: LoggerContext) {
    }

    override fun onLevelChange(logger: Logger, level: Level) {
    }
}