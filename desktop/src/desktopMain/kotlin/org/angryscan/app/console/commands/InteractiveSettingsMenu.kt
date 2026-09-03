package org.angryscan.app.console.commands

import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.prompt
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.MatchersRegister
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.UserSignatureSettings
import org.angryscan.app.scan.common.writer.ResultWriter
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.angryscan.common.matchers.UserSignature
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

/**
 * Interactive settings menu for console mode.
 *
 * Navigation:
 * - Up/Down: change selection
 * - Right/Enter/Space: open submenu / activate item
 * - Left: go back
 * - Escape: request application exit
 */
class InteractiveSettingsMenu constructor(
    private val prompter: SelectListPrompter,
    private val appSettings: AppSettings,
    private val scanSettings: ScanSettings,
    private val userSignatureSettings: UserSignatureSettings,
    private val userSignatureFormPrompter: UserSignatureFormPrompter = ThrowingUserSignatureFormPrompter(),
    private val directoryPrompter: DirectoryPrompter = ThrowingDirectoryPrompter(),
    private val filePrompter: FilePrompter = ThrowingFilePrompter(),
) : KoinComponent {
    /**
     * Thrown to abort interactive menu immediately (e.g. Ctrl+C).
     */
    class AbortException : RuntimeException()

    /**
     * Thrown to request application exit (e.g. Escape).
     */
    class ExitRequestedException : RuntimeException()

    interface SelectListPrompter {
        fun select(entries: List<String>, title: String, startingIndex: Int): String?
        fun multiSelect(
            entries: List<String>,
            title: String,
            initialSelected: Set<String>,
            startingIndex: Int,
        ): Set<String>?
    }

    private class TerminalSelectListPrompter(private val terminal: Terminal) : SelectListPrompter {
        override fun select(entries: List<String>, title: String, startingIndex: Int): String? {
            return ViewportSelectListPrompter(terminal).select(entries, title, startingIndex)
        }

        override fun multiSelect(
            entries: List<String>,
            title: String,
            initialSelected: Set<String>,
            startingIndex: Int,
        ): Set<String>? {
            return ViewportSelectListPrompter(terminal).multiSelect(
                entries = entries,
                title = title,
                initialSelected = initialSelected,
                startingIndex = startingIndex,
            )
        }
    }

    interface UserSignatureFormPrompter {
        /**
         * Edit a user signature.
         *
         * @param existing Existing signature to edit, or null for creating a new one.
         * @param reservedNames Names that cannot be used (case-sensitive), usually all other signatures.
         * @return Saved signature or null if canceled.
         */
        fun edit(existing: UserSignature?, reservedNames: Set<String>): UserSignature?
    }

    private class ThrowingUserSignatureFormPrompter : UserSignatureFormPrompter {
        override fun edit(existing: UserSignature?, reservedNames: Set<String>): UserSignature? {
            error("UserSignatureFormPrompter is not configured for this InteractiveSettingsMenu instance")
        }
    }

    interface DirectoryPrompter {
        /**
         * @return Directory path or null if canceled.
         */
        fun promptDirectory(title: String, initial: String): String?
    }

    interface FilePrompter {
        /**
         * @return File path or null if canceled.
         */
        fun promptFile(title: String, initial: String): String?
    }

    private class ThrowingDirectoryPrompter : DirectoryPrompter {
        override fun promptDirectory(title: String, initial: String): String? {
            error("DirectoryPrompter is not configured for this InteractiveSettingsMenu instance")
        }
    }

    private class ThrowingFilePrompter : FilePrompter {
        override fun promptFile(title: String, initial: String): String? {
            error("FilePrompter is not configured for this InteractiveSettingsMenu instance")
        }
    }

    private class TerminalDirectoryPrompter(private val terminal: Terminal) : DirectoryPrompter {
        override fun promptDirectory(title: String, initial: String): String? {
            WindowsConsoleUtf8.ensureUtf8()
            return terminal.prompt(prompt = title, default = initial, showDefault = false)
        }
    }

    private class TerminalFilePrompter(private val terminal: Terminal) : FilePrompter {
        override fun promptFile(title: String, initial: String): String? {
            WindowsConsoleUtf8.ensureUtf8()
            return terminal.prompt(prompt = title, default = initial, showDefault = false)
        }
    }

    constructor(
        terminal: Terminal,
        appSettings: AppSettings,
        scanSettings: ScanSettings,
        userSignatureSettings: UserSignatureSettings,
    ) : this(
        prompter = TerminalSelectListPrompter(terminal),
        appSettings = appSettings,
        scanSettings = scanSettings,
        userSignatureSettings = userSignatureSettings,
        userSignatureFormPrompter = TerminalUserSignatureFormPrompter(terminal),
        directoryPrompter = TerminalDirectoryPrompter(terminal),
        filePrompter = TerminalFilePrompter(terminal),
    )

    private var hasUnsavedChanges: Boolean = false
    private var baseline: Snapshot = Snapshot.capture(appSettings, scanSettings, userSignatureSettings)
    private val lastSelectedByPage: MutableMap<Page, String> = mutableMapOf()

    private val appSettingsFile: AppSettings.AppSettingsFile by inject()
    private val scanSettingsFile: ScanSettings.SettingsFile by inject()
    private val userSignaturesFile: UserSignatureSettings.SettingsFile by inject()

    fun run() {
        // Use Mordant animations everywhere to avoid leaving screen history.
        var page: Page = Page.Main
        val stack = ArrayDeque<Page>()
        while (true) {
            try {
                when (page) {
                    Page.Main -> {
                        val selection = selectFromPairs(
                            title = "Settings",
                            entries = buildList {
                                add("app" to "Application Settings →")
                                add("scan" to "Scan Settings →")
                                add("io" to "Import/Export →")
                                // Save must be shown only when there are real changes, and above Exit.
                                if (hasUnsavedChanges) add("save" to "Save settings")
                                add("exit" to "Exit")
                            },
                            defaultId = lastSelectedByPage[Page.Main],
                        ) ?: continue
                        lastSelectedByPage[Page.Main] = selection
                        when (selection) {
                            "app" -> {
                                stack.addLast(page)
                                page = Page.App
                            }

                            "scan" -> {
                                stack.addLast(page)
                                page = Page.Scan
                            }

                            "io" -> {
                                stack.addLast(page)
                                page = Page.ImportExport
                            }

                            "save" -> saveSettings()
                            "exit" -> if (confirmExit()) break
                        }
                    }

                    Page.App -> {
                        val selection = selectFromPairs(
                            title = "Application Settings",
                            entries = listOf(
                                "thread" to "Thread count: ${appSettings.threadCount.value}",
                                "report" to "Report extension: ${appSettings.reportSaveExtension.value.extension}",
                                "back" to "Back",
                            ),
                            defaultId = lastSelectedByPage[Page.App],
                        )
                        if (selection == null || selection == "back") {
                            page = stack.removeLastOrNull() ?: Page.Main
                            continue
                        }
                        lastSelectedByPage[Page.App] = selection
                        when (selection) {
                            "thread" -> editThreadCount()
                            "report" -> editReportExtension()
                        }
                    }

                    Page.Scan -> {
                        val selection = selectFromPairs(
                            title = "Scan Settings",
                            entries = listOf(
                                "extensions" to "Extensions: ${scanSettings.extensions.size}",
                                "matchers" to "Matchers: ${scanSettings.matchers.size}",
                                "signatures" to "User signatures: ${scanSettings.userSignatures.size}",
                                "fast" to "Fast scan: ${scanSettings.fastScan.value}",
                                "engine" to "Engine: ${engineName()}",
                                "back" to "Back",
                            ),
                            defaultId = lastSelectedByPage[Page.Scan],
                        )
                        if (selection == null || selection == "back") {
                            page = stack.removeLastOrNull() ?: Page.Main
                            continue
                        }
                        lastSelectedByPage[Page.Scan] = selection
                        when (selection) {
                            "extensions" -> editExtensions()
                            "matchers" -> editMatchers()
                            "signatures" -> {
                                stack.addLast(page)
                                page = Page.UserSignatures
                            }
                            "fast" -> toggleFastScan()
                            "engine" -> editEngine()
                        }
                    }

                    Page.UserSignatures -> {
                        val selection = selectFromPairs(
                            title = "User signatures",
                            entries = listOf(
                                "select" to "Select used signatures: ${scanSettings.userSignatures.size}",
                                "manage" to "Manage user signatures →",
                                "back" to "Back",
                            ),
                            defaultId = lastSelectedByPage[Page.UserSignatures],
                        )
                        if (selection == null || selection == "back") {
                            page = stack.removeLastOrNull() ?: Page.Scan
                            continue
                        }
                        lastSelectedByPage[Page.UserSignatures] = selection
                        when (selection) {
                            "select" -> selectActiveUserSignatures()
                            "manage" -> {
                                stack.addLast(page)
                                page = Page.ManageUserSignatures
                            }
                        }
                    }

                    Page.ManageUserSignatures -> {
                        val selection = selectFromPairs(
                            title = "Manage user signatures",
                            entries = listOf(
                                "add" to "Add signature",
                                "edit" to "Edit signature",
                                "delete" to "Delete signature",
                                "back" to "Back",
                            ),
                            defaultId = lastSelectedByPage[Page.ManageUserSignatures],
                        )
                        if (selection == null || selection == "back") {
                            page = stack.removeLastOrNull() ?: Page.UserSignatures
                            continue
                        }
                        lastSelectedByPage[Page.ManageUserSignatures] = selection
                        when (selection) {
                            "add" -> addUserSignature()
                            "edit" -> editUserSignature()
                            "delete" -> deleteUserSignature()
                        }
                    }

                    Page.ImportExport -> {
                        val selection = selectFromPairs(
                            title = "Import/Export",
                            entries = listOf(
                                "export_all" to "Export all (app + scan + signatures)",
                                "import_all" to "Import all (app + scan + signatures)",
                                "export_app" to "Export AppSettings (file)",
                                "import_app" to "Import AppSettings (file)",
                                "export_scan" to "Export ScanSettings (file)",
                                "import_scan" to "Import ScanSettings (file)",
                                "export_sig" to "Export user signatures (file)",
                                "import_sig" to "Import user signatures (file)",
                                "back" to "Back",
                            ),
                            defaultId = lastSelectedByPage[Page.ImportExport],
                        )
                        if (selection == null || selection == "back") {
                            page = stack.removeLastOrNull() ?: Page.Main
                            continue
                        }
                        lastSelectedByPage[Page.ImportExport] = selection

                        when (selection) {
                            "export_all" -> exportAllToDirectory()
                            "import_all" -> importAllFromDirectory()
                            "export_app" -> exportSingleToFile(kind = "app")
                            "import_app" -> importSingleFromFile(kind = "app")
                            "export_scan" -> exportSingleToFile(kind = "scan")
                            "import_scan" -> importSingleFromFile(kind = "scan")
                            "export_sig" -> exportSingleToFile(kind = "signatures")
                            "import_sig" -> importSingleFromFile(kind = "signatures")
                        }
                    }
                }
            } catch (_: ExitRequestedException) {
                if (confirmExit()) break
            } catch (_: AbortException) {
                // User aborted interactive session (e.g. Ctrl+C).
                return
            }
        }
    }

    // --- Editing actions ---

    private fun editThreadCount() {
        val maxThreads = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val values = (1..maxThreads).map { it.toString() }
        val current = appSettings.threadCount.value.toString()
        val startingIndex = values.indexOf(current).coerceAtLeast(0)
        val selected = prompter.select(values, title = "Thread count", startingIndex = startingIndex)
        val parsed = selected?.toIntOrNull()
        if (parsed != null && parsed != appSettings.threadCount.value) {
            appSettings.threadCount.value = parsed
        }
        updateDirty()
    }

    private fun editReportExtension() {
        val values = ResultWriter.FileExtensions.entries.map { it.extension }
        val current = appSettings.reportSaveExtension.value.extension
        val startingIndex = values.indexOf(current).coerceAtLeast(0)
        val selected = prompter.select(values, title = "Report extension", startingIndex = startingIndex)
        val mapped = selected?.let { ext -> ResultWriter.FileExtensions.entries.find { it.extension == ext } }
        if (mapped != null && mapped != appSettings.reportSaveExtension.value) {
            appSettings.reportSaveExtension.value = mapped
        }
        updateDirty()
    }

    private fun editExtensions() {
        val all = ScanCliFileTypes.selectableFileTypes()
        val selected = selectMulti(
            title = "Extensions",
            allIds = all.map { it.name.replace(" ", "_") },
            initialSelected = scanSettings.extensions.map { it.name.replace(" ", "_") }.toSet(),
        ) ?: return

        if (selected.isNotEmpty()) {
            val resolved = selected.mapNotNull { id -> all.find { it.name.replace(" ", "_") == id } }
            if (resolved.isNotEmpty()) {
                scanSettings.extensions.clear()
                scanSettings.extensions.addAll(resolved)
            }
        }
        updateDirty()
    }

    private fun editMatchers() {
        val all = MatchersRegister.toList()
        val selected = selectMulti(
            title = "Matchers",
            allIds = all.map { it.name.replace(" ", "_") },
            initialSelected = scanSettings.matchers.map { it.name.replace(" ", "_") }.toSet(),
        ) ?: return

        if (selected.isNotEmpty()) {
            val resolved = selected.mapNotNull { id -> all.find { it.name.replace(" ", "_") == id } }
            if (resolved.isNotEmpty()) {
                scanSettings.matchers.clear()
                scanSettings.matchers.addAll(resolved)
            }
        }
        updateDirty()
    }

    private fun selectActiveUserSignatures() {
        val all = userSignatureSettings.userSignatures
        val selected = selectMulti(
            title = "User signatures",
            allIds = all.map { it.name.replace(" ", "_") },
            initialSelected = scanSettings.userSignatures.map { it.name.replace(" ", "_") }.toSet(),
        ) ?: return

        // Keep behavior consistent with other multi-select settings: do not overwrite the
        // current configuration with an empty selection.
        if (selected.isNotEmpty()) {
            val resolved = selected.mapNotNull { id -> all.find { it.name.replace(" ", "_") == id } }
            if (resolved.isNotEmpty()) {
                scanSettings.userSignatures.clear()
                scanSettings.userSignatures.addAll(resolved)
            }
        }
        updateDirty()
    }

    private fun addUserSignature() {
        val reserved = userSignatureSettings.userSignatures.map { it.name }.toSet()
        val created = userSignatureFormPrompter.edit(existing = null, reservedNames = reserved) ?: return

        if (userSignatureSettings.userSignatures.any { it.name == created.name }) {
            // Should be prevented by the editor, but keep a safe guard.
            prompter.select(entries = listOf("OK"), title = "Error: signature '${created.name}' already exists", startingIndex = 0)
            return
        }

        userSignatureSettings.userSignatures.add(created)
        // Keep behavior consistent with UI: add and select immediately.
        scanSettings.userSignatures.add(created)
        updateDirty()
    }

    private fun editUserSignature() {
        val all = userSignatureSettings.userSignatures
        if (all.isEmpty()) {
            prompter.select(entries = listOf("OK"), title = "No user signatures to edit", startingIndex = 0)
            return
        }

        val toEditId = prompter.select(
            entries = all.map { it.name.replace(" ", "_") } + "Back",
            title = "Select signature to edit",
            startingIndex = 0,
        ) ?: return

        if (toEditId == "Back") return

        val existing = all.firstOrNull { it.name.replace(" ", "_") == toEditId } ?: return
        val reserved = all.map { it.name }.filter { it != existing.name }.toSet()

        val updated = userSignatureFormPrompter.edit(existing = existing, reservedNames = reserved) ?: return
        val index = all.indexOfFirst { it.name == existing.name }
        if (index < 0) return

        val wasSelected = scanSettings.userSignatures.any { it.name == existing.name }

        // Replace definition.
        all[index] = updated

        // Keep ScanSettings selection consistent.
        scanSettings.userSignatures.removeIf { it.name == existing.name }
        scanSettings.userSignatures.removeIf { it.name == updated.name }
        if (wasSelected) scanSettings.userSignatures.add(updated)

        // Remove any signatures that no longer exist in settings.
        scanSettings.userSignatures.removeIf { sig ->
            userSignatureSettings.userSignatures.none { it.name == sig.name }
        }

        updateDirty()
    }

    private fun deleteUserSignature() {
        val all = userSignatureSettings.userSignatures
        if (all.isEmpty()) {
            prompter.select(entries = listOf("OK"), title = "No user signatures to delete", startingIndex = 0)
            return
        }

        val toDeleteId = prompter.select(
            entries = all.map { it.name.replace(" ", "_") } + "Back",
            title = "Select signature to delete",
            startingIndex = 0,
        ) ?: return
        if (toDeleteId == "Back") return
        val existing = all.firstOrNull { it.name.replace(" ", "_") == toDeleteId } ?: return

        val confirm = prompter.select(
            entries = listOf("Delete", "Cancel"),
            title = "Delete '${existing.name}'?",
            startingIndex = 1,
        ) ?: return
        if (confirm != "Delete") return

        userSignatureSettings.userSignatures.remove(existing)
        scanSettings.userSignatures.removeIf { it.name == existing.name }
        updateDirty()
    }

    private fun toggleFastScan() {
        scanSettings.fastScan.value = !scanSettings.fastScan.value
        updateDirty()
    }

    private fun editEngine() {
        val values = listOf("HyperScan", "Kotlin")
        val current = engineName()
        val startingIndex = values.indexOf(current).coerceAtLeast(0)
        val selected = prompter.select(values, title = "Engine", startingIndex = startingIndex)
        val mapped = when (selected) {
            "HyperScan" -> HyperScanEngine::class
            "Kotlin" -> KotlinEngine::class
            else -> null
        }
        if (mapped != null && mapped != scanSettings.engine.value) {
            scanSettings.engine.value = mapped
        }
        updateDirty()
    }

    private fun updateDirty() {
        hasUnsavedChanges = Snapshot.capture(appSettings, scanSettings, userSignatureSettings) != baseline
    }

    private fun saveSettings() {
        if (!hasUnsavedChanges) return
        appSettings.save()
        scanSettings.save()
        userSignatureSettings.save()
        baseline = Snapshot.capture(appSettings, scanSettings, userSignatureSettings)
        hasUnsavedChanges = false
    }

    private fun confirmExit(): Boolean {
        val title = if (hasUnsavedChanges) "Exit?" else "Exit?"
        val entries = if (hasUnsavedChanges) {
            listOf("Save and exit", "Exit", "Cancel")
        } else {
            listOf("Exit", "Cancel")
        }

        val choice = try {
            // Default to Cancel.
            prompter.select(entries = entries, title = title, startingIndex = entries.lastIndex)
        } catch (_: ExitRequestedException) {
            // Esc inside confirmation should act like Cancel.
            null
        } ?: return false

        return when (choice) {
            "Save and exit" -> {
                saveSettings()
                true
            }

            "Exit" -> true
            "Cancel" -> false
            else -> false
        }
    }

    private fun engineName(): String {
        return when (scanSettings.engine.value) {
            HyperScanEngine::class -> "HyperScan"
            KotlinEngine::class -> "Kotlin"
            else -> scanSettings.engine.value.simpleName ?: "Unknown"
        }
    }

    private enum class Page { Main, App, Scan, UserSignatures, ManageUserSignatures, ImportExport }

    // --- Import / Export ---

    private fun exportAllToDirectory() {
        // Ensure disk state matches UI.
        if (hasUnsavedChanges) saveSettings() else persistAll()

        val dirRaw = directoryPrompter.promptDirectory(title = "Export directory", initial = "")?.trim().orEmpty()
        if (dirRaw.isEmpty()) return

        val dir = Path(dirRaw)
        Files.createDirectories(dir)
        if (!dir.isDirectory()) {
            prompter.select(entries = listOf("OK"), title = "Export failed: not a directory", startingIndex = 0)
            return
        }

        val files = listOf(appSettingsFile, scanSettingsFile, userSignaturesFile)
        for (f in files) {
            val src = Path(f.path)
            val dst = dir.resolve(src.name)
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING)
        }

        prompter.select(entries = listOf("OK"), title = "Export completed", startingIndex = 0)
    }

    private fun exportSingleToFile(kind: String) {
        // Persist latest state before exporting.
        when (kind) {
            "app" -> appSettings.save()
            "scan" -> scanSettings.save()
            "signatures" -> userSignatureSettings.save()
        }

        val defaultName = when (kind) {
            "app" -> "AppSettings.json"
            "scan" -> "ScanSettings.json"
            "signatures" -> "UserSignatures.json"
            else -> "settings.json"
        }

        val fileRaw = filePrompter.promptFile(title = "Export file", initial = defaultName)?.trim().orEmpty()
        if (fileRaw.isEmpty()) return

        val dest = Path(fileRaw)
        if (dest.exists() && dest.isDirectory()) {
            prompter.select(entries = listOf("OK"), title = "Export failed: expected a file path", startingIndex = 0)
            return
        }
        dest.parent?.let { Files.createDirectories(it) }

        val src = Path(sourceFileForKind(kind).path)
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING)

        prompter.select(entries = listOf("OK"), title = "Export completed", startingIndex = 0)
    }

    private fun importAllFromDirectory() {
        if (!confirmDiscardUnsavedChangesIfNeeded()) return

        val dirRaw = directoryPrompter.promptDirectory(title = "Import directory", initial = "")?.trim().orEmpty()
        if (dirRaw.isEmpty()) return

        val dir = Path(dirRaw)
        if (!dir.exists() || !dir.isDirectory()) {
            prompter.select(entries = listOf("OK"), title = "Import failed: directory not found", startingIndex = 0)
            return
        }

        fun copyExpected(target: java.io.File) {
            val targetPath = Path(target.path)
            val src = dir.resolve(targetPath.name)
            if (!src.exists()) {
                prompter.select(entries = listOf("OK"), title = "Import failed: missing ${targetPath.name}", startingIndex = 0)
                throw IllegalStateException("Missing file: ${targetPath.name}")
            }
            Files.copy(src, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }

        try {
            copyExpected(appSettingsFile)
            copyExpected(scanSettingsFile)
            copyExpected(userSignaturesFile)
        } catch (_: IllegalStateException) {
            return
        }

        userSignatureSettings.reload()
        scanSettings.reload()
        appSettings.reload()

        baseline = Snapshot.capture(appSettings, scanSettings, userSignatureSettings)
        hasUnsavedChanges = false

        prompter.select(entries = listOf("OK"), title = "Import completed", startingIndex = 0)
    }

    private fun importSingleFromFile(kind: String) {
        if (!confirmDiscardUnsavedChangesIfNeeded()) return

        val defaultName = when (kind) {
            "app" -> "AppSettings.json"
            "scan" -> "ScanSettings.json"
            "signatures" -> "UserSignatures.json"
            else -> "settings.json"
        }

        val fileRaw = filePrompter.promptFile(title = "Import file", initial = defaultName)?.trim().orEmpty()
        if (fileRaw.isEmpty()) return

        val src = Path(fileRaw)
        if (!src.exists() || src.isDirectory()) {
            prompter.select(entries = listOf("OK"), title = "Import failed: file not found", startingIndex = 0)
            return
        }

        val target = Path(sourceFileForKind(kind).path)
        Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING)

        when (kind) {
            "app" -> appSettings.reload()
            "scan" -> scanSettings.reload()
            "signatures" -> {
                userSignatureSettings.reload()
                syncScanUserSignaturesWithDefinitions()
            }
        }

        baseline = Snapshot.capture(appSettings, scanSettings, userSignatureSettings)
        hasUnsavedChanges = false

        prompter.select(entries = listOf("OK"), title = "Import completed", startingIndex = 0)
    }

    private fun confirmDiscardUnsavedChangesIfNeeded(): Boolean {
        if (!hasUnsavedChanges) return true
        val confirm = prompter.select(
            entries = listOf("Import (discard unsaved changes)", "Cancel"),
            title = "Import will overwrite files",
            startingIndex = 1,
        ) ?: return false
        return confirm.startsWith("Import")
    }

    private fun persistAll() {
        appSettings.save()
        scanSettings.save()
        userSignatureSettings.save()
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
        updateDirty()
    }

    private data class Snapshot(
        val threadCount: Int,
        val reportExtension: ResultWriter.FileExtensions,
        val fastScan: Boolean,
        val engine: kotlin.reflect.KClass<*>,
        val extensions: List<String>,
        val matchers: List<String>,
        val userSignatures: List<String>,
        val userSignatureDefinitions: List<String>,
    ) {
        companion object {
            fun capture(app: AppSettings, scan: ScanSettings, userSignatureSettings: UserSignatureSettings): Snapshot {
                fun id(s: String): String = s.replace(" ", "_")
                return Snapshot(
                    threadCount = app.threadCount.value,
                    reportExtension = app.reportSaveExtension.value,
                    fastScan = scan.fastScan.value,
                    engine = scan.engine.value,
                    extensions = scan.extensions.map { id(it.name) }.sorted(),
                    matchers = scan.matchers.map { id(it.name) }.sorted(),
                    userSignatures = scan.userSignatures.map { id(it.name) }.sorted(),
                    userSignatureDefinitions = userSignatureSettings.userSignatures.map { us ->
                        val sigs = us.searchSignatures.joinToString("|")
                        "${id(us.name)}=$sigs"
                    }.sorted(),
                )
            }
        }
    }

    private fun selectFromPairs(
        title: String,
        entries: List<Pair<String, String>>,
        defaultId: String? = null,
    ): String? {
        // `interactiveSelectList` returns the selected string, or null if cancelled.
        val labelToId = LinkedHashMap<String, String>(entries.size)
        val labels = entries.map { (id, label) ->
            // Ensure stable uniqueness for mapping.
            val unique = if (labelToId.containsKey(label)) "$label " else label
            labelToId[unique] = id
            unique
        }

        val startingIndex = if (defaultId == null) {
            0
        } else {
            entries.indexOfFirst { (id, _) -> id == defaultId }.let { idx ->
                if (idx < 0) 0 else idx
            }
        }

        val selectedLabel = prompter.select(labels, title = title, startingIndex = startingIndex) ?: return null
        return labelToId[selectedLabel]
    }

    private fun selectMulti(
        title: String,
        allIds: List<String>,
        initialSelected: Set<String>,
    ): Set<String>? {
        // Multi-select is handled by the prompter itself (space toggles, exit applies).
        return prompter.multiSelect(
            entries = allIds,
            title = title,
            initialSelected = initialSelected,
            startingIndex = 0,
        )
    }
}
