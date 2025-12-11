package org.angryscan.app.console.commands

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import io.github.oshai.kotlinlogging.KotlinLogging
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.MatchersRegister
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.UserSignatureSettings
import org.angryscan.app.scan.common.files.types.CertFileType
import org.angryscan.app.scan.common.files.types.CodeFileType
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.app.scan.common.writer.ResultWriter
import org.angryscan.common.engine.IScanEngine
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

class Settings : SuspendingCliktCommand(
    name = "settings"
), KoinComponent {
    
    override fun help(context: com.github.ajalt.clikt.core.Context): String {
        return "View and modify application settings (AppSettings and ScanSettings)\u0085\u0085" +
                "Examples:\u0085" +
                "  settings                                    # View current settings\u0085" +
                "  settings --thread-count 4                   # Set thread count to 4\u0085" +
                "  settings --report-extension csv             # Set report extension to CSV\u0085" +
                "  settings --extensions Text,PDF              # Set file extensions\u0085" +
                "  settings --matchers Email,Phone             # Set matchers\u0085" +
                "  settings --fast                             # Enable fast scan\u0085" +
                "  settings --engine HyperScan                 # Set scan engine"
    }
    
    val userSignatureSettings: UserSignatureSettings by inject()
    val scanSettings by inject<ScanSettings>()
    val appSettings by inject<AppSettings>()

    // AppSettings options
    val threadCount by option(
        "-tc", "--thread-count",
        help = "Number of threads to use for scanning (1 to ${Runtime.getRuntime().availableProcessors()})"
    )
        .int()
        .validate {
            val maxThreads = Runtime.getRuntime().availableProcessors()
            require(it in 1..maxThreads) {
                "Thread count must be between 1 and $maxThreads"
            }
        }

    val reportExtension by option(
        "-re", "--report-extension",
        help = "File extension for reports \u0085" +
                "Available extensions: \u0085" +
                ResultWriter.FileExtensions.entries.joinToString("\u0085") {
                    "- ${it.extension}"
                }
    )
        .choice(
            *ResultWriter.FileExtensions.entries.map { it.extension }.toTypedArray()
        )
        .convert { inputValue ->
            ResultWriter.FileExtensions.entries.find { it.extension == inputValue }
                ?: throw PrintMessage("Unknown extension: $inputValue")
        }

    // ScanSettings options
    val extensions by option(
        "-e", "--extensions",
        help = "Comma separated list of file extensions to scan \u0085" +
                "Supported extensions: \u0085" +
                IFileType
                    .getAll()
                    .filter { it !in (CertFileType.entries + CodeFileType.entries) }
                    .joinToString("\u0085")
                    { "- ${it.name.replace(" ", "_")} (${it.extensions().joinToString(",")})" }
    )
        .convert { inputValue ->
            IFileType
                .getAll()
                .filterNot {
                    it in (CertFileType.entries + CodeFileType.entries)
                }
                .find { it.name.replace(" ", "_") == inputValue }
                ?: throw PrintMessage("Unknown extension: $inputValue")
        }
        .split(",")
        .validate {
            require(it.isNotEmpty()) {
                "At least one extension must be specified"
            }
        }

    val matchers by option(
        "-m", "--matchers",
        help = "Comma separated list of matchers to use \u0085" +
                "Supported matchers: \u0085" +
                MatchersRegister
                    .joinToString("\u0085") {
                        "- ${it.name.replace(" ", "_")}"
                    }
    )
        .convert { inputValue ->
            MatchersRegister
                .find { it.name.replace(" ", "_") == inputValue }
                ?: throw PrintMessage("Unknown matcher: $inputValue")
        }
        .split(",")
        .validate {
            require(it.isNotEmpty()) {
                "At least one matcher must be specified"
            }
        }

    val userSignatures by option(
        "-us", "--user-signatures",
        help = "Comma separated list of user signatures to use \u0085" +
                "Available user signatures: \u0085" +
                userSignatureSettings
                    .userSignatures
                    .joinToString("\u0085") {
                        "- ${it.name.replace(" ", "_")}"
                    }
    )
        .convert { inputValue ->
            userSignatureSettings
                .userSignatures
                .find { it.name.replace(" ", "_") == inputValue }
                ?: throw PrintMessage("Unknown user signature: $inputValue")
        }
        .split(",")
        .validate {
            require(it.isNotEmpty()) {
                "At least one user signature must be specified"
            }
        }

    val fastScan by option().switch(
        "--fast" to true,
        "--full" to false,
    )

    val engine by option(
        "-eng", "--engine",
        help = "Scan engine to use \u0085" +
                "Available engines: \u0085" +
                "- HyperScan\u0085" +
                "- Kotlin"
    )
        .choice("HyperScan", "Kotlin")
        .convert { inputValue ->
            when (inputValue) {
                "HyperScan" -> HyperScanEngine::class
                "Kotlin" -> KotlinEngine::class
                else -> throw PrintMessage("Unknown engine: $inputValue")
            }
        }

    override suspend fun run() {
        var hasChanges = false

        // Display current settings if no options provided
        if (threadCount == null &&
            reportExtension == null &&
            extensions == null &&
            matchers == null &&
            userSignatures == null &&
            fastScan == null &&
            engine == null
        ) {
            displayCurrentSettings()
            return
        }

        // Update AppSettings
        threadCount?.let { value ->
            val oldValue = appSettings.threadCount.value
            appSettings.threadCount.value = value
            echo("Thread count: $oldValue -> $value")
            hasChanges = true
        }

        reportExtension?.let { value ->
            val oldValue = appSettings.reportSaveExtension.value
            appSettings.reportSaveExtension.value = value
            echo("Report extension: ${oldValue.extension} -> ${value.extension}")
            hasChanges = true
        }

        // Update ScanSettings
        extensions?.let { value ->
            scanSettings.extensions.clear()
            scanSettings.extensions.addAll(value)
            echo("Extensions: ${value.joinToString(", ") { it.name }}")
            hasChanges = true
        }

        matchers?.let { value ->
            scanSettings.matchers.clear()
            scanSettings.matchers.addAll(value)
            echo("Matchers: ${value.joinToString(", ") { it.name }}")
            hasChanges = true
        }

        userSignatures?.let { value ->
            scanSettings.userSignatures.clear()
            scanSettings.userSignatures.addAll(value)
            echo("User signatures: ${value.joinToString(", ") { it.name }}")
            hasChanges = true
        }

        fastScan?.let { value ->
            val oldValue = scanSettings.fastScan.value
            scanSettings.fastScan.value = value
            echo("Fast scan: $oldValue -> $value")
            hasChanges = true
        }

        engine?.let { value ->
            val oldValue = scanSettings.engine.value
            scanSettings.engine.value = value
            echo("Engine: ${getEngineName(oldValue)} -> ${getEngineName(value)}")
            hasChanges = true
        }

        // Save changes
        if (hasChanges) {
            try {
                appSettings.save()
                scanSettings.save()
                echo("Settings saved successfully")
            } catch (e: Exception) {
                logger.error(e) { "Failed to save settings" }
                throw PrintMessage("Failed to save settings: ${e.message}")
            }
        } else {
            echo("No changes to save")
        }
    }

    private fun displayCurrentSettings() {
        echo("=== Application Settings (AppSettings) ===")
        echo("Thread count: ${appSettings.threadCount.value}")
        echo("Report extension: ${appSettings.reportSaveExtension.value.extension}")
        echo("")
        echo("=== Scan Settings (ScanSettings) ===")
        echo("Extensions: ${scanSettings.extensions.joinToString(", ") { it.name }}")
        echo("Matchers: ${scanSettings.matchers.joinToString(", ") { it.name }}")
        echo("User signatures: ${scanSettings.userSignatures.joinToString(", ") { it.name }}")
        echo("Fast scan: ${scanSettings.fastScan.value}")
        echo("Engine: ${getEngineName(scanSettings.engine.value)}")
        echo("")
        echo("Use --help to see available options for modification")
    }

    private fun getEngineName(engineClass: KClass<out IScanEngine>): String {
        return when (engineClass) {
            HyperScanEngine::class -> "HyperScan"
            KotlinEngine::class -> "Kotlin"
            else -> engineClass.simpleName ?: "Unknown"
        }
    }
}
