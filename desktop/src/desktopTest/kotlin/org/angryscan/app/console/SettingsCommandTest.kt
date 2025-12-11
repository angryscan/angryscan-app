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
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

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
}
