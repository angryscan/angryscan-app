package org.angryscan.app.ui

import org.angryscan.app.ui.components.SelectionTypes
import java.io.File

/**
 * Resolves the path string passed into [org.angryscan.app.scan.ScanService.createTask]
 * for the file-share UI source.
 *
 * Important: a single `.csv`/`.txt` file must NOT imply [SelectionTypes.FileWithPaths].
 * That mode is explicit (user picked "file with paths"). Otherwise a data CSV is
 * misread as thousands of fake paths and the task appears to scan forever.
 *
 * Manual entry of a path-list file also requires [SelectionTypes.FileWithPaths]
 * (via the dedicated picker); File/Folder mode always scans the selected path itself.
 */
object FileShareScanPaths {

    data class Resolved(
        val scanPath: String,
        val selectionType: SelectionTypes,
        /** When mode is FileWithPaths, the list file path (for task name). */
        val listFilePath: String? = null,
        /** Lines read from a path-list file (non-blank). Zero in File/Folder mode. */
        val listedPathCount: Int = 0,
        /** Listed paths that do not exist on disk. UI should surface this when > 0. */
        val missingPathCount: Int = 0,
    )

    /**
     * UI icon/placeholder hint only. Must not drive scan-path expansion by itself.
     */
    fun guessUiSelectionType(rawPath: String, explicitType: SelectionTypes): SelectionTypes {
        if (rawPath.isBlank()) return explicitType
        val parts = rawPath.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size == 1) {
            val single = File(parts.first())
            if (single.isDirectory) return SelectionTypes.Folder
            if (single.isFile) {
                if (explicitType == SelectionTypes.FileWithPaths &&
                    single.extension.lowercase() in PATH_LIST_EXTENSIONS
                ) {
                    return SelectionTypes.FileWithPaths
                }
                return SelectionTypes.File
            }
        }
        if (parts.size > 1) return SelectionTypes.File
        return explicitType
    }

    fun resolve(rawPath: String, selectionType: SelectionTypes): Resolved {
        val normalized = rawPath
            .split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(";")

        return when (selectionType) {
            SelectionTypes.FileWithPaths -> {
                val listFile = File(normalized)
                val listedPaths = listFile
                    .takeIf { it.isFile }
                    ?.readLines()
                    .orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                val existingPaths = listedPaths.filter { File(it).exists() }
                Resolved(
                    scanPath = existingPaths.joinToString(";"),
                    selectionType = SelectionTypes.FileWithPaths,
                    listFilePath = normalized,
                    listedPathCount = listedPaths.size,
                    missingPathCount = listedPaths.size - existingPaths.size,
                )
            }
            SelectionTypes.Folder, SelectionTypes.File -> Resolved(
                scanPath = normalized,
                selectionType = selectionType,
            )
        }
    }

    private val PATH_LIST_EXTENSIONS = setOf("txt", "csv")
}
