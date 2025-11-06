package org.angryscan.app

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import org.angryscan.app.common.AppFiles
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.LogMarkers
import org.angryscan.app.common.OS
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.UserSignatureSettings
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.TaskFileResult
import org.angryscan.app.scan.TaskFilesViewModel
import org.angryscan.app.scan.common.connectors.ConnectorFileShare
import org.angryscan.app.scan.common.files.FileType
import org.angryscan.app.ui.strings.readableName
import org.angryscan.app.ui.windows.screens.scans.components.SortColumn
import org.angryscan.app.ui.windows.screens.scans.components.comparator
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.angryscan.common.extensions.Matchers
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

object Console : KoinComponent {
    lateinit var progressBar: ProgressBar

    private var path: String? = null
    private var reportDir: File = AppFiles.UserDirPath
    private var reportEncoding =
        if (OS.currentOS() == OS.WINDOWS)
            "windows-1251"
        else
            "UTF-8"
    private var fileWithPaths: Boolean = false

    suspend fun consoleRun(args: Array<String>) {

        parseArgs(args)

        val scanSettings by inject<ScanSettings>()

        val scanService by inject<ScanService>()

        if (path != null) {
            logger.info(throwable = null, LogMarkers.UserAction) {
                "Starting scanning with path: $path " +
                        "extensions: ${scanSettings.extensions.joinToString(", ")} " +
                        "detect matchers: ${scanSettings.matchers.joinToString(", ")} " +
                        "user signatures: ${scanSettings.userSignatures.joinToString(", ")} " +
                        "fast scan: ${scanSettings.fastScan} "
            }
            println("Selecting files...")

            val scanPath = if (fileWithPaths) {
                val file = File(path!!)
                file.readLines().joinToString(separator = ";")
            } else {
                path
            }
            val matchers =
                (scanSettings.matchers + scanSettings.userSignatures)
                    .toMutableList()

            val task = scanService.createTask(
                name = if (fileWithPaths) path else null,
                path = scanPath!!,
                extensions = scanSettings.extensions,
                matchers = matchers,
                fastScan = scanSettings.fastScan.value,
                connector = ConnectorFileShare()
            )

            scanService.startTask(task)

            println("Searching files for scan")
            var scanStarted = false

            CoroutineScope(Dispatchers.Default).launch {
                while (true) {
                    if (task.state.value == TaskState.SCANNING) {
                        task.checkProgress()
                        if (!scanStarted) {

                            progressBar = ProgressBarBuilder()
                                .setStyle(ProgressBarStyle.ASCII)
                                .setTaskName("Scanning")
                                .setInitialMax(task.selectedFiles.value)
                                .build()
                            scanStarted = true
                        }
                        progressBar.stepTo(task.scannedFiles.value + task.skippedFiles.value)
                    }

                    if (task.state.value == TaskState.COMPLETED ||
                        task.state.value == TaskState.FAILED
                    ) {
                        break
                    }
                    delay(500)
                }
            }.join()
            val taskFiles by inject<TaskFilesViewModel> { parametersOf(task.dbTask) }
            println("Generating report...")
            while (!taskFiles.updated.value) {
                delay(500)
            }
            val saveReportPath = saveReport(taskFiles)
            if (saveReportPath != null) {
                logger.info(
                    throwable = null,
                    LogMarkers.UserAction
                ) { "Scanning completed. Report saved to $saveReportPath" }
            } else {
                logger.error(throwable = null) { "Error when saving report" }
            }


            println("Program completed")
            exitProcess(0)
        } else
            println("Chose path to scan with -path parameter or file with paths with -file parameter")
    }

    private fun saveReport(taskFiles: TaskFilesViewModel): String? {
        println("Saving report...")
        val time = Calendar.getInstance().time
        val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
        val currentTime = formatter.format(time)
        val reportFile = reportDir.resolve("ADS_$currentTime.csv")

        return runBlocking {
            try {
                writeCSV(
                    reportFile = reportFile,
                    reportEncoding = reportEncoding,
                    result = taskFiles.taskFiles.value.sortedWith(
                        SortColumn.Score.comparator()
                    )
                )
                reportFile.absolutePath
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun writeCSV(reportFile: File, reportEncoding: String = "UTF-8", result: List<TaskFileResult>) {
        withContext(Dispatchers.IO) {
            FileOutputStream(reportFile, true).bufferedWriter(charset = Charset.forName(reportEncoding))
        }.use { writer ->
            val columns = listOf(
                "File",
                "Attributes",
                "Score",
                "Count",
                "Size"
            )
            writer.append(
                columns.joinToString(";") + "\r\n"
            )

            result.forEach { fileRow ->
                writer.append(
                    listOf(
                        fileRow.path,
                        fileRow.foundAttributes.joinToString(", ") { attr -> attr.name },
                        fileRow.score.toString(),
                        fileRow.count.toString(),
                        fileRow.size.toString()
                    ).joinToString(";") + "\r\n"
                )
            }
        }
    }

    private fun parseArgs(args: Array<String>) {

        fun getArg(map: Map<String, List<String>>, tag: String, default: Boolean): Boolean =
            if (map.containsKey(tag)) !default else default

        fun getArg(map: Map<String, List<String>>, tag: String, shortTag: String, default: String?): String? =
            map[tag]?.first() ?: map[shortTag]?.first() ?: default

        fun getArg(map: Map<String, List<String>>, tag: String, shortTag: String, default: Int): Int =
            map[tag]?.first()?.toIntOrNull() ?: map[shortTag]?.first()?.toIntOrNull() ?: default

        val map = args.fold(Pair(emptyMap<String, List<String>>(), "")) { (map, lastKey), elem ->
            if (elem.startsWith("-")) Pair(map + (elem to emptyList()), elem)
            else Pair(map + (lastKey to map.getOrDefault(lastKey, emptyList()) + elem), lastKey)
        }.first

        if (map.containsKey("-fast") && map.containsKey("-full")) {
            println("You can't use both -fast and -full")
            exitProcess(1)
        }

        val scanSettings: ScanSettings by inject()
        val appSettings: AppSettings by inject()
        val userSignaturesSettings: UserSignatureSettings by inject()

        val filePath = getArg(map, "-file", "-f", null)

        path = getArg(map, "-path", "-p", filePath)
        val fileExtensions = getArg(map, "-extensions", "-e", scanSettings.extensions.joinToString(","))
        val matchers = getArg(map, "-detect_functions", "-df", scanSettings.matchers.joinToString(","))
        val userSignatures = getArg(map, "-user_signatures", "-us", scanSettings.userSignatures.joinToString(","))
        val fastScan = getArg(map, "-fast", scanSettings.fastScan.value)
        val fullScan = getArg(map, "-full", !fastScan)
        val threadCount = getArg(map, "threads", "-t", appSettings.threadCount.value)
        val reportPath = getArg(map, "-report", "-r", null)
        val encoding = getArg(map, "-report_encoding", "-re", null)
        val engine = getArg(map, "-engine", "-e", "hyperscan")

        if (encoding != null) {
            reportEncoding = encoding
            println("Report encoding: $reportEncoding")
        }

        if (filePath != null)
            fileWithPaths = true

        if (path == null) {
            println("Chose path to scan with -path or -file parameter")
            exitProcess(1)
        } else if (path != filePath && fileWithPaths) {
            println("Only one of the parameters (-path or -file) allowed. Not both.")
        } else {
            println("Selected directory: $path")
        }

        if (reportPath != null) {
            val rpFile = File(reportPath)
            if (rpFile.exists()) {
                if (!rpFile.isDirectory)
                    reportDir = rpFile
                else {
                    println("Report path must be a directory")
                    exitProcess(1)
                }
            } else {
                if (rpFile.mkdirs())
                    reportDir = rpFile
                else {
                    println("Can't create report directory")
                    exitProcess(1)
                }
            }
            println("Report dir: ${reportDir.absolutePath}")
        } else {
            println("Report path not specified, using default: ${reportDir.absolutePath}")
        }

        scanSettings.fastScan.value = !fullScan
        if (scanSettings.fastScan.value)
            println("Fast scan enabled")
        else
            println("Full scan enabled")
        if (threadCount > Runtime.getRuntime().availableProcessors()) {
            println(
                "Thread count can't be greater than available processors (${
                    Runtime.getRuntime().availableProcessors()
                })"
            )
            println("Using ${Runtime.getRuntime().availableProcessors()} instead")
            appSettings.threadCount.value = Runtime.getRuntime().availableProcessors()
        } else if (threadCount < 1) {
            println("Thread count can't be less than 1")
            println("Using 1 instead")
            appSettings.threadCount.value = 1
        } else {
            println("Thread count: $threadCount. Max: ${Runtime.getRuntime().availableProcessors()}")
            appSettings.threadCount.value = threadCount
        }

        if (fileExtensions != null) {
            scanSettings.extensions.clear()
            fileExtensions.split(",").forEach { ext ->
                val extension = FileType.entries.find { it.name == ext }
                if (extension != null)
                    scanSettings.extensions.add(extension)
                else
                    println("Unknown file extension: $ext, skipping...")
            }
        }
        println("Extensions: ${scanSettings.extensions.joinToString(", ")}")

        if (matchers != null) {
            scanSettings.matchers.clear()
            if (matchers.isNotEmpty()) {
                matchers.split(",").forEach { matcher ->
                    val dfo = Matchers.find { it.name.lowercase().replace(' ', '_') == matcher }
                    if (dfo != null)
                        scanSettings.matchers.add(dfo)
                    else
                        println("Unknown detect matcher: $matcher, skipping...")
                }
            }
        }
        println("Detect matchers: ${scanSettings.matchers.joinToString(", ")}")

        if (userSignatures != null) {
            scanSettings.userSignatures.clear()
            if (userSignatures.isNotEmpty()) {
                userSignatures.split(",").forEach { sig ->
                    val sigo =
                        userSignaturesSettings.userSignatures.find { it.name.lowercase().replace(' ', '_') == sig }
                    if (sigo != null)
                        scanSettings.userSignatures.add(sigo)
                    else
                        println("Unknown user detect signature: $sig, skipping...")
                }
            }
        }
        println("User signature matchers: ${scanSettings.userSignatures.joinToString(", ")}")
        if (engine != null) {
            scanSettings.engine.value = when (engine) {
                "hyperscan" -> {
                    println("Scan engine: $engine")
                    HyperScanEngine::class
                }

                "kotlin" -> {
                    println("Scan engine: $engine")
                    KotlinEngine::class
                }

                else -> {
                    println("Unknown engine: $engine, using hyperscan")
                    HyperScanEngine::class
                }
            }
        }
    }

    fun help() {
        val userSignatureSettings: UserSignatureSettings by inject()
        val scanSettings: ScanSettings by inject()
        println(
            """
Allowed parameters:
-path(-p) [path] - path to scan
-file(-f) [path] - file with paths to scan
-extensions(-e) [extensions] - comma-separated list of file extensions
-detect_functions(-df) [detect matchers] - comma-separated list of detect matchers
-user_signatures(-us) [user signatures] - comma-separated list of user detect signatures
-fast - fast scan
-full - full scan
-console(-c) - console mode
-report(-r) [path] - path to dir to save report
-report_encoding(-re) [encoding] - report encoding (UTF-8, Windows-1251) (default: UTF-8)
-threads(-t) [count] - count of threads
-engine(-e) [engine] - scan engine (hyperscan, kotlin) (default: ${runBlocking { scanSettings.engine.value.readableName() }})

Allowed extensions: 
        ${
                FileType.entries.filter { ft -> ft != FileType.CERT && ft != FileType.CODE }
                    .joinToString("\n        ") {
                        "${it.name} (${
                            it.extensions.filter { ext ->
                                ext.toIntOrNull() == null
                            }.joinToString(",")
                        })"
                    }
            }

Allowed detect matchers: 
        ${Matchers.joinToString("\n        ") { it.name.lowercase().replace(' ', '_') }}
Allowed user detect signatures:
        ${userSignatureSettings.userSignatures.joinToString("\n        ") { it.name.lowercase().replace(' ', '_') }} 
            """.trimIndent()
        )
    }
}
