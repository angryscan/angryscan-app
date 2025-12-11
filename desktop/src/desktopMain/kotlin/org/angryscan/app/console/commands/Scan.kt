package org.angryscan.app.console.commands

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.update
import com.github.ajalt.mordant.widgets.progress.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.angryscan.app.common.AppFiles
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.LogMarkers
import org.angryscan.app.common.MatchersRegister
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.UserSignatureSettings
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.TaskFilesViewModel
import org.angryscan.app.scan.common.connectors.ConnectorFileShare
import org.angryscan.app.scan.common.files.types.CertFileType
import org.angryscan.app.scan.common.files.types.CodeFileType
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.app.scan.common.writer.ResultWriter
import org.angryscan.app.ui.windows.screens.scans.components.SortColumn
import org.angryscan.app.ui.windows.screens.scans.components.comparator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

class Scan : SuspendingCliktCommand(), KoinComponent {
    val userSignatureSettings: UserSignatureSettings by inject()
    val scanSettings by inject<ScanSettings>()
    val appSettings by inject<AppSettings>()

    val matchers by option(
        "-m", "--matchers",
        help = "Comma separated list of matchers to use \u0085" +
                "Supported matchers: \u0085\n" +
                MatchersRegister
                    .joinToString("\n") {
                        "- ${it.name.replace(" ", "_")}"
                    }
    )
        .convert { inputValue ->
            MatchersRegister
                .find { it.name.replace(" ", "_") == inputValue }
                ?: throw PrintMessage("Unknown matcher: $inputValue")
        }
        .split(",")
        .default(
            value = scanSettings.matchers,
            defaultForHelp = "Loaded from settings"
        )

    val userSignatures by option(
        "-us", "--user-signatures",
        help = "Comma separated list of user signatures to use \u0085" +
                "Existed user signatures: \u0085\n" +
                userSignatureSettings
                    .userSignatures
                    .joinToString("\n") {
                        "- ${it.name.replace(" ", "_")}"
                    }
    )
        .convert { inputValue ->
            userSignatureSettings
                .userSignatures
                .find { it.name.replace(" ", "_") == inputValue }
                ?: throw PrintMessage("Unknown matcher: $inputValue")
        }
        .split(",")
        .default(
            value = scanSettings.userSignatures,
            defaultForHelp = "Loaded from settings"
        )

    val extensions by option(
        "-e", "--extensions",
        help = "Comma separated list of file extensions to scan \u0085" +
                "Supported extensions: \u0085\n" +
                IFileType
                    .getAll()
                    .filter { it !in (CertFileType.entries + CodeFileType.entries) }
                    .joinToString("\n")
                    { "- ${it.name.replace(" ", "_")} (${it.extensions().joinToString(",")})" }
    )
        .convert { inputValue ->
            IFileType
                .getAll()
                .filterNot {
                    it !in (CertFileType.entries +
                            CodeFileType.entries)
                }
                .find { it.name.replace(" ", "_") == inputValue }
                ?: throw PrintMessage("Unknown extension: $inputValue")
        }
        .split(",")
        .default(
            value = scanSettings.extensions,
            defaultForHelp = "Loaded from settings"
        )

    val paths by option(
        "-p", "--path",
        help = "Paths to scan separated by semicolons"
    )
        .file(
            mustExist = true,
            mustBeReadable = true
        )
        .split(";")
        .required()


    val filesWithPaths by option(
        "-l", "--list",
        help = "Paths represents to files with paths to scan"
    ).flag()

    val reportPath by option(
        "-r", "--report",
        help = "Path to save report"
    )
        .path(
            mustExist = true,
            canBeFile = false,
            canBeDir = true,
            mustBeWritable = true
        )
        .default(
            value = AppFiles.UserDirPath.toPath(),
            defaultForHelp = "Loaded from settings"
        )

    val reportSaveExtension by option(
        "-re", "--report-extension",
        help = "Extension to save report \u0085"
    )
        .choice(
            *ResultWriter.FileExtensions.entries.map { it.extension }.toTypedArray()
        )
        .convert { inputValue ->
            ResultWriter.FileExtensions.entries.find { it.extension == inputValue }
                ?: throw PrintMessage("Unknown extension: $inputValue")
        }.default(
            value = appSettings.reportSaveExtension.value,
            defaultForHelp = "Loaded from settings"
        )

    val fastScan by option().switch(
        "--fast" to true,
        "--full" to false,
    ).default(scanSettings.fastScan.value)

    override suspend fun run() {

        val scanService by inject<ScanService>()

        val scanPaths = if (filesWithPaths) {
            paths.map { f ->
                f.readLines()
                    .joinToString(";")
            }
                .joinToString { ";" }
        } else
            paths.joinToString(";")

        val progressBar = progressBarContextLayout<String> {
            text { "Status: $context" }
            progressBar(
                pendingChar = " ",
                completeChar = "=",
                separatorChar = ">",
            )
            percentage()
            completed()
        }.animateInCoroutine(terminal, context = "Creating task")

        logger.info(throwable = null, LogMarkers.UserAction) {
            "Starting scanning with path: ${paths.joinToString(";")} " +
                    "extensions: ${extensions.joinToString(", ")} " +
                    "matchers: ${matchers.joinToString(", ")} " +
                    "user signatures: ${userSignatures.joinToString(", ")} " +
                    "fast scan: $fastScan "
        }

        CoroutineScope(Dispatchers.Default).launch {
            progressBar.execute()
        }

        val task = scanService.createTask(
            name = if (filesWithPaths) paths.first().path else null,
            path = scanPaths,
            extensions = extensions,
            matchers = matchers,
            fastScan = fastScan,
            connector = ConnectorFileShare()
        )

        progressBar.update {
            context = "Searching files"
        }

        scanService.startTask(task)

        var failed = false

        CoroutineScope(Dispatchers.Default).launch {
            var scanStarted = false

            while (true) {
                if (task.state.value == TaskState.SCANNING) {
                    task.checkProgress()
                    if (!scanStarted) {
                        progressBar.update {
                            context = "Scanning"
                            total = task.selectedFiles.value
                        }
                        scanStarted = true
                    }
                    progressBar.update(
                        completed = task.scannedFiles.value + task.skippedFiles.value
                    )
                }

                if (task.state.value == TaskState.FAILED) {
                    failed = true
                    break
                } else if (task.state.value == TaskState.COMPLETED) {
                    break
                }
                delay(500)
            }

            progressBar.update {
                context = "Completed"
            }
            progressBar.stop()
        }.join()

        val taskFiles by inject<TaskFilesViewModel> { parametersOf(task.dbTask) }

        if (failed) {
            echo("Scan failed")
            exitProcess(1)
        }

        echo("Generating report...")
        while (!taskFiles.updated.value) {
            delay(500)
        }
        val reportPath = saveReport(taskFiles)
        echo("Report saved to $reportPath")
    }

    private suspend fun saveReport(taskFiles: TaskFilesViewModel): String {
        echo("Saving report...")
        val time = Calendar.getInstance().time
        val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
        val currentTime = formatter.format(time)
        val reportFile = reportPath.resolve("ADS_$currentTime.${reportSaveExtension.extension}")

        require(reportFile != null) {
            PrintMessage("Report path is required")
        }

        ResultWriter.saveResult(
            filePath = reportFile.absolutePathString(),
            result = taskFiles.taskFiles.value.sortedWith(
                SortColumn.Score.comparator()
            ),
            onSaveError = { error ->
                throw PrintMessage(error)
            }
        )
        return reportFile.absolutePathString()
    }
}