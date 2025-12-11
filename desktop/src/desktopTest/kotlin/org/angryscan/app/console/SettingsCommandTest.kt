package org.angryscan.app.console

import com.github.ajalt.clikt.command.parse
import kotlinx.coroutines.runBlocking
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.UserSignatureSettings
import org.angryscan.app.console.commands.Settings
import org.angryscan.app.scan.common.files.types.CertFileType
import org.angryscan.app.scan.common.files.types.CodeFileType
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.app.scan.common.writer.ResultWriter
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.junit.After
import org.junit.Before
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.io.path.listDirectoryEntries
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.angryscan.common.matchers.UserSignature

internal class SettingsCommandTest {
    private data class CommandRun(
        val stdout: String,
        val stderr: String,
        val error: Throwable?
    )

    private lateinit var tempDirPath: java.nio.file.Path
    private lateinit var appSettingsPath: java.nio.file.Path
    private lateinit var scanSettingsPath: java.nio.file.Path
    private lateinit var userSignaturesPath: java.nio.file.Path

    @Before
    fun setUp() {
        tempDirPath = createTempDirectory(prefix = "settings-command-test-")

        appSettingsPath = tempDirPath.resolve("AppSettings.json")
        scanSettingsPath = tempDirPath.resolve("ScanSettings.json")
        userSignaturesPath = tempDirPath.resolve("UserSignatures.json")

        // Make files exist so settings load errors are deterministic.
        appSettingsPath.writeText("")
        scanSettingsPath.writeText("")
        userSignaturesPath.writeText("")

        startKoin {
            modules(
                module {
                    single { UserSignatureSettings.SettingsFile(userSignaturesPath.toString()) }
                    single { UserSignatureSettings() }
                    single { AppSettings.AppSettingsFile(appSettingsPath.toString()) }
                    single { AppSettings() }
                    single { ScanSettings.SettingsFile(scanSettingsPath.toString()) }
                    single { ScanSettings() }
                }
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        tempDirPath.toFile().deleteRecursively()
    }

    private fun runSettings(args: List<String>): CommandRun {
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val oldOut = System.out
        val oldErr = System.err
        var error: Throwable? = null

        try {
            System.setOut(PrintStream(stdout, true, Charsets.UTF_8))
            System.setErr(PrintStream(stderr, true, Charsets.UTF_8))

            runBlocking {
                try {
                    Settings().parse(args)
                } catch (t: Throwable) {
                    error = t
                }
            }
        } finally {
            System.setOut(oldOut)
            System.setErr(oldErr)
        }

        return CommandRun(
            stdout = stdout.toString(Charsets.UTF_8),
            stderr = stderr.toString(Charsets.UTF_8),
            error = error
        )
    }

    @Test
    fun `settings without args prints current settings`() = runBlocking {
        val result = runSettings(emptyList())
        assertEquals(null, result.error)
        assertContains(result.stdout, "=== Application Settings (AppSettings) ===")
        assertContains(result.stdout, "=== Scan Settings (ScanSettings) ===")
        assertContains(result.stdout, "Thread count:")
        assertContains(result.stdout, "Report extension:")
        assertContains(result.stdout, "Fast scan:")
        assertContains(result.stdout, "Engine:")
    }

    @Test
    fun `settings can update thread count and saves AppSettings`() = runBlocking {
        val result = runSettings(listOf("--thread-count", "1"))
        assertEquals(null, result.error)
        assertContains(result.stdout, "Thread count:")
        assertContains(result.stdout, "Settings saved successfully")

        assertTrue(appSettingsPath.exists())
        val saved = appSettingsPath.readText()
        assertContains(saved, "\"threadCount\":1")
    }

    @Test
    fun `settings validates thread count range`() = runBlocking {
        val result = runSettings(listOf("--thread-count", "0"))
        val error = requireNotNull(result.error)
        assertTrue((error.message ?: "").contains("Thread count must be between 1"))
    }

    @Test
    fun `settings can update report extension`() = runBlocking {
        val result = runSettings(listOf("--report-extension", "csv"))
        assertEquals(null, result.error)
        assertContains(result.stdout, "Report extension:")

        val saved = appSettingsPath.readText()
        // Serialized as enum name, not file extension.
        assertContains(saved, "\"reportSaveExtension\":\"${ResultWriter.FileExtensions.CSV.name}\"")
    }

    @Test
    fun `settings can update scan extensions`() = runBlocking {
        val fileType = IFileType
            .getAll()
            .first { it !in (CertFileType.entries + CodeFileType.entries) }

        val arg = fileType.name.replace(" ", "_")

        val result = runSettings(listOf("--extensions", arg))
        assertEquals(null, result.error)
        assertContains(result.stdout, "Extensions:")
        assertTrue(scanSettingsPath.readText().isNotBlank())
    }

    @Test
    fun `settings can update fast scan flag`() = runBlocking {
        val result = runSettings(listOf("--fast"))
        assertEquals(null, result.error)
        assertContains(result.stdout, "Fast scan:")
        assertContains(result.stdout, "Settings saved successfully")
    }

    @Test
    fun `settings can switch scan engine`() = runBlocking {
        // Force a change regardless of OS default.
        val result = runSettings(listOf("--engine", "HyperScan"))
        assertEquals(null, result.error)
        assertContains(result.stdout, "Engine:")

        val saved = scanSettingsPath.readText()
        // Polymorphic serialization includes class information; just ensure it references the chosen engine.
        assertTrue(saved.contains(HyperScanEngine::class.qualifiedName ?: "HyperScanEngine") ||
            saved.contains("HyperScan"))

        val result2 = runSettings(listOf("--engine", "Kotlin"))
        assertEquals(null, result2.error)

        val saved2 = scanSettingsPath.readText()
        assertTrue(saved2.contains(KotlinEngine::class.qualifiedName ?: "KotlinEngine") ||
            saved2.contains("Kotlin"))
    }

    @Test
    fun `settings interactive mode requires a real console`() = runBlocking {
        val result = runSettings(listOf("--interactive"))
        val error = requireNotNull(result.error)
        assertTrue((error.message ?: "").contains("Interactive mode"), "Expected interactive mode error, got: ${error.message}")
    }

    @Test
    fun `settings can add user signature and saves UserSignatures and ScanSettings`() = runBlocking {
        val result = runSettings(
            listOf(
                "--user-signature-add",
                "--user-signature-name",
                "MySig",
                "--user-signature-signature",
                "AAA,BBB",
            )
        )
        assertEquals(null, result.error)
        assertContains(result.stdout, "Settings saved successfully")

        val savedUserSignatures = userSignaturesPath.readText()
        assertContains(savedUserSignatures, "MySig")
        assertContains(savedUserSignatures, "AAA")
        assertContains(savedUserSignatures, "BBB")

        val savedScanSettings = scanSettingsPath.readText()
        assertContains(savedScanSettings, "MySig")
    }

    @Test
    fun `settings can replace user signature signatures`() = runBlocking {
        // Seed existing signature in settings file.
        val us = UserSignatureSettings()
        us.userSignatures.clear()
        us.userSignatures.add(UserSignature(name = "MySig", searchSignatures = mutableListOf("OLD")))
        us.save()

        val result = runSettings(
            listOf(
                "--user-signature-replace",
                "--user-signature-name",
                "MySig",
                "--user-signature-signature",
                "NEW1,NEW2",
            )
        )
        assertEquals(null, result.error)
        assertContains(result.stdout, "Settings saved successfully")

        val savedUserSignatures = userSignaturesPath.readText()
        assertContains(savedUserSignatures, "MySig")
        assertTrue(!savedUserSignatures.contains("OLD"), "Expected old signature values to be removed")
        assertContains(savedUserSignatures, "NEW1")
        assertContains(savedUserSignatures, "NEW2")
    }

    @Test
    fun `settings can remove user signature`() = runBlocking {
        // Seed existing signature in settings file.
        val us = UserSignatureSettings()
        us.userSignatures.clear()
        us.userSignatures.add(UserSignature(name = "ToDelete", searchSignatures = mutableListOf("A")))
        us.save()

        val result = runSettings(
            listOf(
                "--user-signature-remove",
                "--user-signature-name",
                "ToDelete",
            )
        )
        assertEquals(null, result.error)
        assertContains(result.stdout, "Settings saved successfully")

        val savedUserSignatures = userSignaturesPath.readText()
        assertTrue(!savedUserSignatures.contains("ToDelete"), "Expected signature to be removed from UserSignatures.json")
    }

    @Test
    fun `settings add user signature fails on duplicate name`() = runBlocking {
        val us = UserSignatureSettings()
        us.userSignatures.clear()
        us.userSignatures.add(UserSignature(name = "Dup", searchSignatures = mutableListOf("A")))
        us.save()

        val result = runSettings(
            listOf(
                "--user-signature-add",
                "--user-signature-name",
                "Dup",
                "--user-signature-signature",
                "B",
            )
        )
        val error = requireNotNull(result.error)
        assertTrue((error.message ?: "").contains("already"), "Expected duplicate name error, got: ${error.message}")
    }

    @Test
    fun `settings can export all settings files to a directory`() = runBlocking {
        val exportDir = createTempDirectory(prefix = "settings-export-all-")
        try {
            val result = runSettings(listOf("--export", "all", "--dir", exportDir.toString()))
            assertEquals(null, result.error)
            assertContains(result.stdout, "Export completed")

            val exportedApp = exportDir.resolve("AppSettings.json")
            val exportedScan = exportDir.resolve("ScanSettings.json")
            val exportedSigs = exportDir.resolve("UserSignatures.json")

            assertTrue(exportedApp.exists(), "Expected AppSettings.json to be exported")
            assertTrue(exportedScan.exists(), "Expected ScanSettings.json to be exported")
            assertTrue(exportedSigs.exists(), "Expected UserSignatures.json to be exported")

            // Export copies exact file contents.
            assertEquals(appSettingsPath.readText(), exportedApp.readText())
            assertEquals(scanSettingsPath.readText(), exportedScan.readText())
            assertEquals(userSignaturesPath.readText(), exportedSigs.readText())
        } finally {
            exportDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `settings can export only app settings file`() = runBlocking {
        val exportDir = createTempDirectory(prefix = "settings-export-app-")
        try {
            val exportFile = exportDir.resolve("app-export.json")
            val result = runSettings(listOf("--export", "app", "--file", exportFile.toString()))
            assertEquals(null, result.error)
            assertContains(result.stdout, "Export completed")

            val exported = exportDir.listDirectoryEntries().map { it.fileName.toString() }.sorted()
            assertEquals(listOf("app-export.json"), exported)
            assertEquals(appSettingsPath.readText(), exportFile.readText())
        } finally {
            exportDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `settings can import all settings files from a directory`() = runBlocking {
        val maxThreads = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val firstThreadCount = 1
        val secondThreadCount = if (maxThreads >= 2) 2 else 1

        // Create a known state and export it as a "package".
        val seed1 = runSettings(
            listOf(
                "--thread-count",
                firstThreadCount.toString(),
            )
        )
        assertEquals(null, seed1.error)

        val seed2 = runSettings(
            listOf(
                "--user-signature-add",
                "--user-signature-name",
                "PkgSig",
                "--user-signature-signature",
                "AAA,BBB",
            )
        )
        assertEquals(null, seed2.error)

        val pkgDir = createTempDirectory(prefix = "settings-import-all-pkg-")
        try {
            val exported = runSettings(listOf("--export", "all", "--dir", pkgDir.toString()))
            assertEquals(null, exported.error)

            // Change current settings to ensure import actually replaces files.
            val change1 = runSettings(listOf("--thread-count", secondThreadCount.toString()))
            assertEquals(null, change1.error)
            val change2 = runSettings(
                listOf(
                    "--user-signature-replace",
                    "--user-signature-name",
                    "PkgSig",
                    "--user-signature-signature",
                    "XXX",
                )
            )
            assertEquals(null, change2.error)

            val beforeImportApp = appSettingsPath.readText()
            val beforeImportScan = scanSettingsPath.readText()
            val beforeImportSigs = userSignaturesPath.readText()

            // Import should replace all three files with the package contents.
            val imported = runSettings(listOf("--import", "all", "--dir", pkgDir.toString()))
            assertEquals(null, imported.error)
            assertContains(imported.stdout, "Import completed")

            val pkgApp = pkgDir.resolve("AppSettings.json").readText()
            val pkgScan = pkgDir.resolve("ScanSettings.json").readText()
            val pkgSigs = pkgDir.resolve("UserSignatures.json").readText()

            assertTrue(beforeImportApp != pkgApp || beforeImportScan != pkgScan || beforeImportSigs != pkgSigs)

            assertEquals(pkgApp, appSettingsPath.readText())
            assertEquals(pkgScan, scanSettingsPath.readText())
            assertEquals(pkgSigs, userSignaturesPath.readText())
        } finally {
            pkgDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `settings can import only user signatures file`() = runBlocking {
        // Prepare a package file with signatures.
        val pkgDir = createTempDirectory(prefix = "settings-import-sigs-pkg-")
        try {
            val seed = runSettings(
                listOf(
                    "--user-signature-add",
                    "--user-signature-name",
                    "OnlySigs",
                    "--user-signature-signature",
                    "S1",
                )
            )
            assertEquals(null, seed.error)

            val pkgFile = pkgDir.resolve("sigs.json")
            val exported = runSettings(listOf("--export", "signatures", "--file", pkgFile.toString()))
            assertEquals(null, exported.error)

            // Mutate signatures to a different valid state.
            val mutate = runSettings(
                listOf(
                    "--user-signature-replace",
                    "--user-signature-name",
                    "OnlySigs",
                    "--user-signature-signature",
                    "S2",
                )
            )
            assertEquals(null, mutate.error)

            // Capture current non-signature files after mutation (signature replace saves app+scan too).
            val appBefore = appSettingsPath.readText()
            val scanBefore = scanSettingsPath.readText()

            // Import only signatures should not modify other files.
            val imported = runSettings(listOf("--import", "signatures", "--file", pkgFile.toString()))
            assertEquals(null, imported.error)

            val pkgSigs = pkgFile.readText()
            assertEquals(pkgSigs, userSignaturesPath.readText())
            assertEquals(appBefore, appSettingsPath.readText())
            assertEquals(scanBefore, scanSettingsPath.readText())
        } finally {
            pkgDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `settings signatures subcommand can add user signature`() = runBlocking {
        val result = runSettings(
            listOf(
                "signatures",
                "add",
                "--name",
                "MySig2",
                "--signature",
                "AAA,BBB",
            )
        )
        assertEquals(null, result.error)
        assertContains(result.stdout, "Settings saved successfully")

        val savedUserSignatures = userSignaturesPath.readText()
        assertContains(savedUserSignatures, "MySig2")
        assertContains(savedUserSignatures, "AAA")
        assertContains(savedUserSignatures, "BBB")
    }

    @Test
    fun `settings signatures subcommand can replace user signature`() = runBlocking {
        val us = UserSignatureSettings()
        us.userSignatures.clear()
        us.userSignatures.add(UserSignature(name = "MySig2", searchSignatures = mutableListOf("OLD")))
        us.save()

        val result = runSettings(
            listOf(
                "signatures",
                "replace",
                "--name",
                "MySig2",
                "--signature",
                "NEW1,NEW2",
            )
        )
        assertEquals(null, result.error)
        assertContains(result.stdout, "Settings saved successfully")

        val savedUserSignatures = userSignaturesPath.readText()
        assertTrue(!savedUserSignatures.contains("OLD"))
        assertContains(savedUserSignatures, "NEW1")
        assertContains(savedUserSignatures, "NEW2")
    }

    @Test
    fun `settings signatures subcommand can remove user signature`() = runBlocking {
        val us = UserSignatureSettings()
        us.userSignatures.clear()
        us.userSignatures.add(UserSignature(name = "ToDelete2", searchSignatures = mutableListOf("A")))
        us.save()

        val result = runSettings(
            listOf(
                "signatures",
                "remove",
                "--name",
                "ToDelete2",
            )
        )
        assertEquals(null, result.error)
        assertContains(result.stdout, "Settings saved successfully")

        val savedUserSignatures = userSignaturesPath.readText()
        assertTrue(!savedUserSignatures.contains("ToDelete2"))
    }
}
