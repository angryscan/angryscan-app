package org.angryscan.app.console.commands

import com.github.ajalt.mordant.terminal.Terminal
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.MatchersRegister
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.UserSignatureSettings
import org.angryscan.app.scan.common.files.types.CertFileType
import org.angryscan.app.scan.common.files.types.CodeFileType
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.app.scan.common.writer.ResultWriter
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine

/**
 * Interactive settings menu for console mode.
 *
 * Navigation:
 * - Up/Down: change selection
 * - Right/Enter/Space: open submenu / activate item
 * - Left: go back
 * - Escape: request application exit
 */
internal class InteractiveSettingsMenu internal constructor(
    private val prompter: SelectListPrompter,
    private val appSettings: AppSettings,
    private val scanSettings: ScanSettings,
    private val userSignatureSettings: UserSignatureSettings,
) {
    /**
     * Thrown to abort interactive menu immediately (e.g. Ctrl+C).
     */
    internal class AbortException : RuntimeException()

    /**
     * Thrown to request application exit (e.g. Escape).
     */
    internal class ExitRequestedException : RuntimeException()

    internal interface SelectListPrompter {
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
    )

    private var hasUnsavedChanges: Boolean = false
    private var baseline: Snapshot = Snapshot.capture(appSettings, scanSettings)
    private val lastSelectedByPage: MutableMap<Page, String> = mutableMapOf()

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
                            "signatures" -> editUserSignatures()
                            "fast" -> toggleFastScan()
                            "engine" -> editEngine()
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
        val all = IFileType
            .getAll()
            .filter { it !in (CertFileType.entries + CodeFileType.entries) }
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

    private fun editUserSignatures() {
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
        hasUnsavedChanges = Snapshot.capture(appSettings, scanSettings) != baseline
    }

    private fun saveSettings() {
        if (!hasUnsavedChanges) return
        appSettings.save()
        scanSettings.save()
        baseline = Snapshot.capture(appSettings, scanSettings)
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

    private enum class Page { Main, App, Scan }

    private data class Snapshot(
        val threadCount: Int,
        val reportExtension: ResultWriter.FileExtensions,
        val fastScan: Boolean,
        val engine: kotlin.reflect.KClass<*>,
        val extensions: List<String>,
        val matchers: List<String>,
        val userSignatures: List<String>,
    ) {
        companion object {
            fun capture(app: AppSettings, scan: ScanSettings): Snapshot {
                fun id(s: String): String = s.replace(" ", "_")
                return Snapshot(
                    threadCount = app.threadCount.value,
                    reportExtension = app.reportSaveExtension.value,
                    fastScan = scan.fastScan.value,
                    engine = scan.engine.value,
                    extensions = scan.extensions.map { id(it.name) }.sorted(),
                    matchers = scan.matchers.map { id(it.name) }.sorted(),
                    userSignatures = scan.userSignatures.map { id(it.name) }.sorted(),
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
