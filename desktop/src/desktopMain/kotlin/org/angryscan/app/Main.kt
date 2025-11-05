package org.angryscan.app

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import ch.qos.logback.classic.Level
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.FileKit
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.angryscan.app.common.AppFiles
import org.angryscan.app.common.AppVersion
import org.angryscan.app.common.LogMarkers
import org.angryscan.app.common.OS
import org.angryscan.app.di.*
import org.angryscan.app.logging.LogLevel
import org.angryscan.app.scan.common.ScanPathHelper
import org.angryscan.app.ui.MainWindow
import org.angryscan.app.ui.tray.DorkTray
import org.angryscan.app.ui.windows.ApplicationErrorWindow
import java.awt.event.WindowEvent
import java.io.File
import java.net.BindException
import javax.swing.UIManager
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalComposeUiApi::class)
suspend fun main(args: Array<String>) {

    if (AppVersion != "Debug") {
        LogLevel.setLoggingLevel(Level.INFO)
    }

    File(System.getProperty("java.io.tmpdir")).listFiles()?.toList()?.asFlow()?.map {
        if (it.name.startsWith("ADS_")) {
            it.delete()
        }
    }?.collect()

    System.setProperty(
        "skiko.renderApi",
        when (OS.currentOS()) {
            OS.WINDOWS -> "OPENGL"
            OS.LINUX -> "OPENGL"
            OS.MAC -> "METAL"
            else -> "OPENGL"
        }
    )
    FileKit.init(appId = "Angry Data Scanner")

    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (_: Exception) {
        logger.warn { "Cannot load system look and feel" }
    }

    val port = 10201
    val selectorManager = SelectorManager(Dispatchers.IO)

    try {
        if (args.isEmpty() ||
            arrayOf("-c", "-console", "-h", "-help").all { !args.contains(it) }
        ) {
            val serverSocket = aSocket(selectorManager).tcp().bind("127.0.0.1", port)
            logger.info { "Server started at port $port" }
            CoroutineScope(Dispatchers.IO).launch {
                while (true) {
                    try {
                        val socket = serverSocket.accept()
                        logger.debug { "Client connected" }
                        val input = socket.openReadChannel()
                        val path = input.readUTF8Line()
                        socket.close()
                        if (path != null && File(path).exists()) {
                            ScanPathHelper.setPath(path)
                            logger.debug { "Path received: $path" }
                            ScanPathHelper.requestFocus()
                        }
                    } catch (e: Exception) {
                        logger.error { "Error when receiving data from client: ${e.message}" }
                    }
                }
            }
        }
    } catch (_: BindException) {
        logger.info { "Application already running!" }
        if (args.isNotEmpty() && args.first().let { it != "-c" && it != "-console" }) {
            val path = args.firstOrNull()
            if (path != null && File(path).exists()) {
                logger.info { "Sending path to running app" }
                val clientSocket = aSocket(selectorManager).tcp().connect("127.0.0.1", port)
                val output = clientSocket.openWriteChannel(autoFlush = true)
                output.writeFully(path.toByteArray())
                clientSocket.close()
                selectorManager.close()
                logger.info { "Path sent to running app" }
            }
        }
        logger.info { "Terminating app" }
        exitProcess(0)
    } catch (e: Exception) {
        logger.error { e.message }
        exitProcess(1)
    }

    var lastError: Throwable? by mutableStateOf(null)

    startKoin { //Start Koin
        modules(
            settingsModule,
            databaseModule,
            scanModule,
            s3Module
        )
    }

    if (args.isNotEmpty() &&
        arrayOf("-c", "-console", "-h", "-help").any { args.contains(it) }
    ) {
        if (arrayOf("-h", "-help").any { args.contains(it) }) {
            Console.help()
        } else if (arrayOf("-c", "-console").any { args.contains(it) }) {
            if (AppFiles.ResultDBFile.exists()) {
                if (!AppFiles.ResultDBFile.delete()) {
                    logger.error { "Cannot access to database. Check it is in use by another process!" }
                    return
                }
            }
            logger.info(throwable = null, LogMarkers.UserAction) { "Starting console application" }
            Console.consoleRun(args)
        }
    } else {
        if (args.isNotEmpty()) {
            logger.warn { "Started with ${args.size} argument(s): ${args.joinToString(", ")}" }
            logger.info { "Set path to: ${args.first()}" }
            ScanPathHelper.setPath(args.first())
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (_: Exception) {
            logger.warn { "Cannot load system look and feel" }
        }

        logger.info(throwable = null, marker = LogMarkers.UserAction) { "Starting GUI application" }
        application(exitProcessOnExit = false) {
            CompositionLocalProvider(
                LocalWindowExceptionHandlerFactory provides WindowExceptionHandlerFactory { window ->
                    WindowExceptionHandler {
                        lastError = it
                        window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
                        throw it
                    }
                }
            ) {
                var mainIsVisible by remember { mutableStateOf(true) }
                
                LaunchedEffect(Unit) {
                    if (OS.currentOS() == OS.MAC) {
                        try {
                            val appClass = Class.forName("com.apple.eawt.Application")
                            val app = appClass.getMethod("getApplication").invoke(null)
                            
                            val listenerClass = Class.forName("com.apple.eawt.AppReOpenedListener")
                            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                                listenerClass.classLoader,
                                arrayOf(listenerClass)
                            ) { _, _, _ ->
                                logger.debug { "macOS dock click detected, restoring window" }
                                mainIsVisible = true
                                null
                            }
                            
                            appClass.getMethod("addAppEventListener", Class.forName("com.apple.eawt.AppEventListener"))
                                .invoke(app, proxy)
                            logger.info { "macOS dock click handler registered successfully" }
                        } catch (e: Exception) {
                            logger.warn { "Could not set up macOS dock handler: ${e.message}" }
                        }
                    }
                }

                MainWindow(
                    onCloseRequest = ::exitApplication,
                    onHideRequest = {
                        mainIsVisible = false
                    },
                    isVisible = mainIsVisible
                )
                DorkTray(
                    mainIsVisible = mainIsVisible,
                    mainVisibilitySet = {
                        mainIsVisible = it
                    }
                )
            }
        }
        if (lastError != null) {
            singleWindowApplication(
                state = WindowState(width = 600.dp, height = 500.dp),
                exitProcessOnExit = false
            ) {
                ApplicationErrorWindow(lastError!!)
            }
            exitProcess(1)
        } else {
            exitProcess(0)
        }
    }
}