package org.angryscan.app.ui

import org.angryscan.app.ui.components.SelectionTypes
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileShareScanPathsTest {

    @Test
    fun `File mode scans csv content path itself not as path list`() {
        val dir = createTempDirectory(prefix = "csv-file-mode-").toFile()
        try {
            val csv = File(dir, "data.csv")
            csv.writeText(
                """
                id,name,note
                1,Alice,/tmp/does-not-exist-a.pdf
                2,Bob,/tmp/does-not-exist-b.pdf
                3,Carol,plain text without slash
                """.trimIndent()
            )

            val resolved = FileShareScanPaths.resolve(csv.absolutePath, SelectionTypes.File)

            assertEquals(SelectionTypes.File, resolved.selectionType)
            assertEquals(csv.absolutePath, resolved.scanPath)
            assertNull(resolved.listFilePath)
            assertEquals(0, resolved.listedPathCount)
            assertEquals(0, resolved.missingPathCount)
            // Data rows must not become scan targets.
            assertEquals(1, resolved.scanPath.split(";").size)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `FileWithPaths expands only existing paths and reports missing count`() {
        val dir = createTempDirectory(prefix = "csv-paths-mode-").toFile()
        try {
            val real1 = File(dir, "a.txt").apply { writeText("a") }
            val real2 = File(dir, "b.txt").apply { writeText("b") }
            val list = File(dir, "paths.csv")
            list.writeText(
                """
                ${real1.absolutePath}
                ${dir.resolve("missing.txt").absolutePath}
                ${real2.absolutePath}
                
                """.trimIndent()
            )

            val resolved = FileShareScanPaths.resolve(list.absolutePath, SelectionTypes.FileWithPaths)

            assertEquals(SelectionTypes.FileWithPaths, resolved.selectionType)
            assertEquals(list.absolutePath, resolved.listFilePath)
            assertEquals(3, resolved.listedPathCount)
            assertEquals(1, resolved.missingPathCount)
            val paths = resolved.scanPath.split(";").filter { it.isNotEmpty() }
            assertEquals(listOf(real1.absolutePath, real2.absolutePath), paths)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `FileWithPaths with only missing paths yields empty scanPath`() {
        val dir = createTempDirectory(prefix = "csv-all-missing-").toFile()
        try {
            val list = File(dir, "empty-targets.txt")
            list.writeText(
                """
                ${dir.resolve("nope-1.bin").absolutePath}
                ${dir.resolve("nope-2.bin").absolutePath}
                """.trimIndent()
            )

            val resolved = FileShareScanPaths.resolve(list.absolutePath, SelectionTypes.FileWithPaths)

            assertEquals("", resolved.scanPath)
            assertEquals(2, resolved.listedPathCount)
            assertEquals(2, resolved.missingPathCount)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `guessUiSelectionType does not force FileWithPaths for csv unless explicit`() {
        val dir = createTempDirectory(prefix = "csv-guess-").toFile()
        try {
            val csv = File(dir, "report.csv").apply { writeText("a,b\n1,2\n") }

            assertEquals(
                SelectionTypes.File,
                FileShareScanPaths.guessUiSelectionType(csv.absolutePath, SelectionTypes.File)
            )
            assertEquals(
                SelectionTypes.FileWithPaths,
                FileShareScanPaths.guessUiSelectionType(csv.absolutePath, SelectionTypes.FileWithPaths)
            )
            assertEquals(
                SelectionTypes.Folder,
                FileShareScanPaths.guessUiSelectionType(dir.absolutePath, SelectionTypes.File)
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `typed csv path in File mode must not expand to data rows`() {
        // Regression guard for the old detectSelectionType auto-FileWithPaths behavior.
        val dir = createTempDirectory(prefix = "csv-legacy-").toFile()
        try {
            val csv = File(dir, "customers.csv")
            val rows = listOf(
                "name,city",
                "Ada,London",
                "Grace,Berlin",
                "/tmp/ghost-path.pdf",
            )
            csv.writeText(rows.joinToString("\n"))

            val asFile = FileShareScanPaths.resolve(csv.absolutePath, SelectionTypes.File)
            val asPathList = FileShareScanPaths.resolve(csv.absolutePath, SelectionTypes.FileWithPaths)

            assertEquals(csv.absolutePath, asFile.scanPath)
            assertTrue(asPathList.listedPathCount >= rows.size)
            // File mode must not produce the multi-target expansion of FileWithPaths.
            assertTrue(asFile.scanPath != asPathList.scanPath || asPathList.scanPath.isEmpty())
            assertEquals(1, asFile.scanPath.split(";").filter { it.isNotEmpty() }.size)
        } finally {
            dir.deleteRecursively()
        }
    }
}
