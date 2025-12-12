package org.angryscan.app.console.commands

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
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
import org.angryscan.common.matchers.UserSignature
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path as KtPath
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

class Settings : SuspendingCliktCommand(
    name = "settings",
), KoinComponent {
    init {
        subcommands(UserSignatures())
    }

    // Clikt: ensure this command still runs without a subcommand.
    override val invokeWithoutSubcommand: Boolean = true
    
    override fun help(context: com.github.ajalt.clikt.core.Context): String {
        return "View and modify application settings (AppSettings and ScanSettings)\u0085\u0085" +
                "Examples:\u0085" +
                "  settings                                    # View current settings\u0085" +
                "  settings --interactive                        # Interactive settings menu\u0085" +
                "  settings --thread-count 4                   # Set thread count to 4\u0085" +
                "  settings --report-extension csv             # Set report extension to CSV\u0085" +
                "  settings --extensions Text,PDF              # Set file extensions\u0085" +
                "  settings --matchers Email,Phone             # Set matchers\u0085" +
                "  settings --fast                             # Enable fast scan\u0085" +
                "  settings --engine HyperScan                 # Set scan engine\u0085" +
                "  settings --user-signature-add --user-signature-name Name --user-signature-signature AAA,BBB   # Add user signature\u0085" +
                "  settings --user-signature-remove --user-signature-name Name                                   # Remove user signature\u0085" +
                "  settings --user-signature-replace --user-signature-name Name --user-signature-signature AAA   # Replace user signature values\u0085" +
                "  settings signatures add --name Name --signature AAA,BBB                                       # Add user signature (subcommand)\u0085" +
                "  settings signatures remove --name Name                                                       # Remove user signature (subcommand)\u0085" +
                "  settings signatures replace --name Name --signature AAA                                       # Replace user signature values (subcommand)"
    }
    
    val userSignatureSettings: UserSignatureSettings by inject()
    val scanSettings by inject<ScanSettings>()
    val appSettings by inject<AppSettings>()

    private val appSettingsFile: AppSettings.AppSettingsFile by inject()
    private val scanSettingsFile: ScanSettings.SettingsFile by inject()
    private val userSignaturesFile: UserSignatureSettings.SettingsFile by inject()

    val interactive by option(
        "-i", "--interactive",
        help = "Run interactive settings menu (requires a real console)"
    ).flag(default = false)

    private val exportKind by option(
        "--export",
        help = "Export settings. Values: all, app, scan, signatures"
    ).choice("all", "app", "scan", "signatures")

    private val importKind by option(
        "--import",
        help = "Import settings (replaces current files). Values: all, app, scan, signatures"
    ).choice("all", "app", "scan", "signatures")

    private val dir by option(
        "--dir",
        help = "Directory path used for --export all / --import all"
    )

    private val file by option(
        "--file",
        help = "File path used for --export app|scan|signatures and --import app|scan|signatures"
    )

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

    // User signature management (non-interactive mode)
    val userSignatureAdd by option(
        "--user-signature-add",
        help = "Add a user signature (requires --user-signature-name and --user-signature-signature)"
    ).flag(default = false)

    val userSignatureRemove by option(
        "--user-signature-remove",
        help = "Remove a user signature (requires --user-signature-name)"
    ).flag(default = false)

    val userSignatureReplace by option(
        "--user-signature-replace",
        help = "Replace a user signature (requires --user-signature-name and --user-signature-signature)"
    ).flag(default = false)

    val userSignatureName by option(
        "--user-signature-name",
        help = "User signature name"
    )

    val userSignatureSignature by option(
        "--user-signature-signature",
        help = "Signature value(s), comma separated"
    )

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
        // If a subcommand is invoked, Clikt will run it. Ensure we don't apply the main settings flow.
        if (currentContext.invokedSubcommand != null) return

        var hasChanges = false
        var hasUserSignatureChanges = false

        val hasTransfer = exportKind != null || importKind != null
        val hasCliEdits =
            threadCount != null ||
                reportExtension != null ||
                extensions != null ||
                matchers != null ||
                userSignatures != null ||
                userSignatureAdd ||
                userSignatureRemove ||
                userSignatureReplace ||
                fastScan != null ||
                engine != null

        if (hasTransfer) {
            if (interactive) {
                throw PrintMessage("Interactive mode cannot be combined with --export/--import")
            }
            if (hasCliEdits) {
                throw PrintMessage("--export/--import cannot be combined with other settings flags")
            }
            if (exportKind != null && importKind != null) {
                throw PrintMessage("Specify only one: --export or --import")
            }

            val kind = exportKind ?: importKind ?: error("unreachable")
            if (kind == "all") {
                val dirRaw = dir?.trim().orEmpty()
                if (dirRaw.isEmpty()) throw PrintMessage("--dir is required for --export all / --import all")
                if (file != null) throw PrintMessage("--file cannot be used with 'all'")

                val dirPath = KtPath(dirRaw)
                if (exportKind != null) {
                    exportAllToDirectory(dirPath)
                } else {
                    importAllFromDirectory(dirPath)
                }
                return
            } else {
                val fileRaw = file?.trim().orEmpty()
                if (fileRaw.isEmpty()) throw PrintMessage("--file is required for --export/--import $kind")
                if (dir != null) throw PrintMessage("--dir cannot be used with '$kind' (use --file)")

                val filePath = KtPath(fileRaw)
                if (exportKind != null) {
                    exportSingleToFile(kind = kind, destFilePath = filePath)
                } else {
                    importSingleFromFile(kind = kind, srcFilePath = filePath)
                }
                return
            }
        }

        if (interactive) {
            if (hasCliEdits) {
                throw PrintMessage("Interactive mode cannot be combined with other settings flags")
            }
            if (System.console() == null) {
                throw PrintMessage("Interactive mode requires a real console")
            }
            InteractiveSettingsMenu(
                terminal = terminal,
                appSettings = appSettings,
                scanSettings = scanSettings,
                userSignatureSettings = userSignatureSettings,
            ).run()
            return
        }

        // Auto interactive menu when no options provided (only when a console is available).
        if (!hasCliEdits) {
            if (System.console() != null) {
                InteractiveSettingsMenu(
                    terminal = terminal,
                    appSettings = appSettings,
                    scanSettings = scanSettings,
                    userSignatureSettings = userSignatureSettings,
                ).run()
            } else {
                displayCurrentSettings()
            }
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

        // Update user signatures (UserSignatureSettings + ScanSettings.userSignatures)
        if (userSignatureAdd || userSignatureRemove || userSignatureReplace) {
            val actions = listOf(userSignatureAdd, userSignatureRemove, userSignatureReplace).count { it }
            if (actions != 1) {
                throw PrintMessage("Specify exactly one of: --user-signature-add, --user-signature-remove, --user-signature-replace")
            }
            val rawName = userSignatureName?.trim().orEmpty()
            if (rawName.isEmpty()) {
                throw PrintMessage("--user-signature-name is required")
            }

            fun matchesName(sig: UserSignature, rawName: String): Boolean {
                return sig.name == rawName || sig.name.replace(" ", "_") == rawName
            }

            fun parseSignatureValues(raw: String): List<String> {
                return raw
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
            }

            if (userSignatureRemove) {
                val removed = userSignatureSettings.userSignatures.removeIf { matchesName(it, rawName) }
                if (!removed) {
                    throw PrintMessage("Unknown user signature: $rawName")
                }
                scanSettings.userSignatures.removeIf { matchesName(it, rawName) }
                echo("User signature removed: $rawName")
                hasUserSignatureChanges = true
                hasChanges = true
            }

            if (userSignatureAdd) {
                val rawSig = userSignatureSignature?.trim().orEmpty()
                if (rawSig.isEmpty()) throw PrintMessage("--user-signature-signature is required")
                val sigs = parseSignatureValues(rawSig)
                if (sigs.isEmpty()) throw PrintMessage("--user-signature-signature must contain at least one value")
                if (userSignatureSettings.userSignatures.any { matchesName(it, rawName) }) {
                    throw PrintMessage("User signature '$rawName' already exists")
                }
                val created = UserSignature(name = rawName, searchSignatures = sigs.toMutableList())
                userSignatureSettings.userSignatures.add(created)
                // Keep behavior consistent with UI: add signature and select it immediately.
                scanSettings.userSignatures.add(created)
                echo("User signature added: $rawName")
                hasUserSignatureChanges = true
                hasChanges = true
            }

            if (userSignatureReplace) {
                val rawSig = userSignatureSignature?.trim().orEmpty()
                if (rawSig.isEmpty()) throw PrintMessage("--user-signature-signature is required")
                val sigs = parseSignatureValues(rawSig)
                if (sigs.isEmpty()) throw PrintMessage("--user-signature-signature must contain at least one value")

                val index = userSignatureSettings.userSignatures.indexOfFirst { matchesName(it, rawName) }
                if (index < 0) {
                    throw PrintMessage("Unknown user signature: $rawName")
                }
                val old = userSignatureSettings.userSignatures[index]
                val wasSelected = scanSettings.userSignatures.any { matchesName(it, old.name) }
                val updated = UserSignature(name = old.name, searchSignatures = sigs.toMutableList())

                userSignatureSettings.userSignatures[index] = updated
                scanSettings.userSignatures.removeIf { matchesName(it, old.name) }
                if (wasSelected) scanSettings.userSignatures.add(updated)

                echo("User signature replaced: ${old.name}")
                hasUserSignatureChanges = true
                hasChanges = true
            }

            // Ensure ScanSettings doesn't reference missing definitions.
            scanSettings.userSignatures.removeIf { sig ->
                userSignatureSettings.userSignatures.none { it.name == sig.name }
            }
        }

        // Save changes
        if (hasChanges) {
            try {
                appSettings.save()
                scanSettings.save()
                if (hasUserSignatureChanges) {
                    userSignatureSettings.save()
                }
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

    private fun exportAllToDirectory(dirPath: Path) {
        // Persist latest state before exporting.
        appSettings.save()
        scanSettings.save()
        userSignatureSettings.save()

        Files.createDirectories(dirPath)
        if (!dirPath.isDirectory()) throw PrintMessage("Target path is not a directory: $dirPath")

        for (f in listOf(appSettingsFile, scanSettingsFile, userSignaturesFile)) {
            val src = KtPath(f.path)
            val dst = dirPath.resolve(src.name)
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING)
        }

        echo("Export completed: all -> $dirPath")
    }

    private fun exportSingleToFile(kind: String, destFilePath: Path) {
        // Persist latest state before exporting.
        when (kind) {
            "app" -> appSettings.save()
            "scan" -> scanSettings.save()
            "signatures" -> userSignatureSettings.save()
        }

        if (destFilePath.exists() && destFilePath.isDirectory()) {
            throw PrintMessage("Expected a file path but got a directory: $destFilePath")
        }

        destFilePath.parent?.let { Files.createDirectories(it) }
        val src = KtPath(sourceFileForKind(kind).path)
        Files.copy(src, destFilePath, StandardCopyOption.REPLACE_EXISTING)

        echo("Export completed: $kind -> $destFilePath")
    }

    private fun importAllFromDirectory(dirPath: Path) {
        if (!dirPath.exists() || !dirPath.isDirectory()) {
            throw PrintMessage("Directory not found: $dirPath")
        }

        fun copyExpectedFile(target: java.io.File) {
            val targetPath = KtPath(target.path)
            val srcPath = dirPath.resolve(targetPath.name)
            if (!srcPath.exists()) throw PrintMessage("Missing file in import directory: $srcPath")
            Files.copy(srcPath, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }

        copyExpectedFile(appSettingsFile)
        copyExpectedFile(scanSettingsFile)
        copyExpectedFile(userSignaturesFile)

        // Reload in-memory state so subsequent usage is consistent.
        userSignatureSettings.reload()
        scanSettings.reload()
        appSettings.reload()

        echo("Import completed: all <- $dirPath")
    }

    private fun importSingleFromFile(kind: String, srcFilePath: Path) {
        if (!srcFilePath.exists()) {
            throw PrintMessage("File not found: $srcFilePath")
        }
        if (srcFilePath.isDirectory()) {
            throw PrintMessage("Expected a file but got a directory: $srcFilePath")
        }

        val targetFile = sourceFileForKind(kind)
        val targetPath = KtPath(targetFile.path)
        Files.copy(srcFilePath, targetPath, StandardCopyOption.REPLACE_EXISTING)

        when (kind) {
            "app" -> appSettings.reload()
            "scan" -> scanSettings.reload()
            "signatures" -> {
                userSignatureSettings.reload()
                syncScanUserSignaturesWithDefinitions()
            }
        }

        echo("Import completed: $kind <- $srcFilePath")
    }

    private fun sourceFileForKind(kind: String): java.io.File {
        return when (kind) {
            "app" -> appSettingsFile
            "scan" -> scanSettingsFile
            "signatures" -> userSignaturesFile
            else -> error("Unknown kind: $kind")
        }
    }

    private fun syncScanUserSignaturesWithDefinitions() {
        val defsByName = userSignatureSettings.userSignatures.associateBy { it.name }
        val selectedNames = scanSettings.userSignatures.map { it.name }
        scanSettings.userSignatures.clear()
        scanSettings.userSignatures.addAll(selectedNames.mapNotNull { defsByName[it] })
    }
}

private class UserSignatures : SuspendingCliktCommand(
    name = "signatures",
), KoinComponent {
    init {
        subcommands(Add(), Remove(), Replace(), ListCmd())
    }

    override fun help(context: com.github.ajalt.clikt.core.Context): String {
        return "Manage user signature definitions (UserSignatures.json)"
    }

    override suspend fun run() {
        // No-op: actions are implemented in subcommands.
    }

    private abstract class Base : SuspendingCliktCommand(), KoinComponent {
        protected val userSignatureSettings: UserSignatureSettings by inject()
        protected val scanSettings: ScanSettings by inject()

        protected fun parseSignatureValues(raw: String): List<String> {
            return raw
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        }

        protected fun matchesName(sig: UserSignature, rawName: String): Boolean {
            return sig.name == rawName || sig.name.replace(" ", "_") == rawName
        }
    }

    private class Add : Base() {
        private val name by option("-n", "--name", help = "Signature name").required()
        private val signature by option("-s", "--signature", help = "Signature value(s), comma separated").required()

        override suspend fun run() {
            val rawName = name.trim()
            if (rawName.isEmpty()) throw PrintMessage("--name cannot be empty")

            val sigs = parseSignatureValues(signature)
            if (sigs.isEmpty()) throw PrintMessage("--signature must contain at least one value")

            if (userSignatureSettings.userSignatures.any { matchesName(it, rawName) }) {
                throw PrintMessage("User signature '$rawName' already exists")
            }

            val created = UserSignature(name = rawName, searchSignatures = sigs.toMutableList())
            userSignatureSettings.userSignatures.add(created)
            // Keep behavior consistent with UI: add and select immediately.
            scanSettings.userSignatures.add(created)

            userSignatureSettings.save()
            scanSettings.save()
            echo("Settings saved successfully")
        }
    }

    private class Remove : Base() {
        private val name by option("-n", "--name", help = "Signature name").required()

        override suspend fun run() {
            val rawName = name.trim()
            if (rawName.isEmpty()) throw PrintMessage("--name cannot be empty")

            val removed = userSignatureSettings.userSignatures.removeIf { matchesName(it, rawName) }
            if (!removed) throw PrintMessage("Unknown user signature: $rawName")

            scanSettings.userSignatures.removeIf { matchesName(it, rawName) }

            userSignatureSettings.save()
            scanSettings.save()
            echo("Settings saved successfully")
        }
    }

    private class Replace : Base() {
        private val name by option("-n", "--name", help = "Signature name").required()
        private val signature by option("-s", "--signature", help = "Signature value(s), comma separated").required()

        override suspend fun run() {
            val rawName = name.trim()
            if (rawName.isEmpty()) throw PrintMessage("--name cannot be empty")

            val sigs = parseSignatureValues(signature)
            if (sigs.isEmpty()) throw PrintMessage("--signature must contain at least one value")

            val index = userSignatureSettings.userSignatures.indexOfFirst { matchesName(it, rawName) }
            if (index < 0) throw PrintMessage("Unknown user signature: $rawName")

            val old = userSignatureSettings.userSignatures[index]
            val wasSelected = scanSettings.userSignatures.any { matchesName(it, old.name) }
            val updated = UserSignature(name = old.name, searchSignatures = sigs.toMutableList())

            userSignatureSettings.userSignatures[index] = updated
            scanSettings.userSignatures.removeIf { matchesName(it, old.name) }
            if (wasSelected) scanSettings.userSignatures.add(updated)

            userSignatureSettings.save()
            scanSettings.save()
            echo("Settings saved successfully")
        }
    }

    private class ListCmd : Base() {
        override suspend fun run() {
            if (userSignatureSettings.userSignatures.isEmpty()) {
                echo("No user signatures")
                return
            }
            echo("User signatures:")
            userSignatureSettings.userSignatures
                .sortedBy { it.name }
                .forEach { echo("- ${it.name}") }
        }
    }
}
