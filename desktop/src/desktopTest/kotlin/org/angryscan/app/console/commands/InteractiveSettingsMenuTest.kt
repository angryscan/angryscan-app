package org.angryscan.app.console.commands

import kotlinx.coroutines.runBlocking
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.MatchersRegister
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.UserSignatureSettings
import org.junit.After
import org.junit.Before
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.ArrayDeque
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class InteractiveSettingsMenuTest {
    private data class Call(val title: String, val entries: List<String>, val startingIndex: Int)

    private class FakePrompter(
        private val decisions: ArrayDeque<(title: String, entries: List<String>, startingIndex: Int) -> String?>,
    ) : InteractiveSettingsMenu.SelectListPrompter {
        val calls: MutableList<Call> = mutableListOf()

        override fun select(entries: List<String>, title: String, startingIndex: Int): String? {
            calls += Call(title = title, entries = entries, startingIndex = startingIndex)
            if (decisions.isEmpty()) error("No scripted decision left for title=$title")
            return decisions.removeFirst().invoke(title, entries, startingIndex)
        }

        override fun multiSelect(
            entries: List<String>,
            title: String,
            initialSelected: Set<String>,
            startingIndex: Int,
        ): Set<String>? {
            error("multiSelect is not used by these tests")
        }
    }

    private class FakePrompterWithMulti(
        private val decisions: ArrayDeque<(title: String, entries: List<String>, startingIndex: Int) -> String?>,
        private val multiDecision: (title: String, entries: List<String>, initialSelected: Set<String>) -> Set<String>?,
    ) : InteractiveSettingsMenu.SelectListPrompter {
        var multiCalls: Int = 0

        override fun select(entries: List<String>, title: String, startingIndex: Int): String? {
            if (decisions.isEmpty()) error("No scripted decision left for title=$title")
            return decisions.removeFirst().invoke(title, entries, startingIndex)
        }

        override fun multiSelect(
            entries: List<String>,
            title: String,
            initialSelected: Set<String>,
            startingIndex: Int,
        ): Set<String>? {
            multiCalls += 1
            return multiDecision(title, entries, initialSelected)
        }
    }

    private lateinit var tempDirPath: java.nio.file.Path

    @Before
    fun setUp() {
        tempDirPath = createTempDirectory(prefix = "interactive-settings-menu-test-")

        val appSettingsPath = tempDirPath.resolve("AppSettings.json")
        val scanSettingsPath = tempDirPath.resolve("ScanSettings.json")
        val userSignaturesPath = tempDirPath.resolve("UserSignatures.json")

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

    @Test
    fun `main menu keeps selection after save action`() = runBlocking {
        val appSettings = AppSettings()
        val scanSettings = ScanSettings()
        val userSignatureSettings = UserSignatureSettings()

        val decisions = ArrayDeque<(String, List<String>, Int) -> String?>()

        // 1) Main menu: choose Save.
        decisions += { title, entries, _ ->
            assertEquals("Settings", title)
            val app = entries.firstOrNull { it.startsWith("Application Settings") }
            assertNotNull(app)
            app
        }

        // 2) App menu: choose Thread count (to make settings dirty)
        decisions += { title, entries, _ ->
            assertEquals("Application Settings", title)
            val thread = entries.firstOrNull { it.startsWith("Thread count:") }
            assertNotNull(thread)
            thread
        }

        // 3) Thread count: choose a different value (if possible)
        decisions += { title, entries, startingIndex ->
            assertEquals("Thread count", title)
            val current = entries[startingIndex]
            val next = entries.firstOrNull { it != current } ?: current
            next
        }

        // 4) Back to App menu: Back
        decisions += { title, entries, _ ->
            assertEquals("Application Settings", title)
            val back = entries.firstOrNull { it == "Back" }
            assertNotNull(back)
            back
        }

        // 5) Main menu: choose Save settings
        decisions += { title, entries, _ ->
            assertEquals("Settings", title)
            val saveIndex = entries.indexOfFirst { it == "Save settings" }
            assertTrue(saveIndex >= 0, "Expected 'Save settings' entry to exist")
            "Save settings"
        }

        // 6) Main menu again (after save): Save settings should disappear, cursor goes to first item
        decisions += { title, entries, startingIndex ->
            assertEquals("Settings", title)
            assertTrue(entries.none { it == "Save settings" }, "Expected 'Save settings' to be hidden after saving")
            assertEquals(0, startingIndex, "Expected cursor to reset to the first entry after 'Save settings' disappears")
            val exit = entries.firstOrNull { it == "Exit" }
            assertNotNull(exit)
            exit
        }

        // 7) Exit confirmation: Exit
        decisions += { title, entries, startingIndex ->
            assertEquals("Exit?", title)
            assertEquals(listOf("Exit", "Cancel"), entries)
            assertEquals(entries.lastIndex, startingIndex)
            "Exit"
        }

        val prompter = FakePrompter(decisions)

        InteractiveSettingsMenu(
            prompter = prompter,
            appSettings = appSettings,
            scanSettings = scanSettings,
            userSignatureSettings = userSignatureSettings,
        ).run()

        // Sanity: we really visited main menu twice.
        assertTrue(prompter.calls.count { it.title == "Settings" } >= 2)
    }

    @Test
    fun `thread count uses current settings as preselected value and keeps selection on return`() = runBlocking {
        val appSettings = AppSettings()
        val scanSettings = ScanSettings()
        val userSignatureSettings = UserSignatureSettings()

        val maxThreads = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val expectedThreadCount = if (maxThreads >= 2) 2 else 1
        appSettings.threadCount.value = expectedThreadCount

        val decisions = ArrayDeque<(String, List<String>, Int) -> String?>()

        // Main menu -> Application Settings
        decisions += { title, entries, _ ->
            assertEquals("Settings", title)
            val app = entries.firstOrNull { it.startsWith("Application Settings") }
            assertNotNull(app)
            app
        }

        // App menu -> Thread count
        decisions += { title, entries, _ ->
            assertEquals("Application Settings", title)
            val thread = entries.firstOrNull { it.startsWith("Thread count:") }
            assertNotNull(thread)
            thread
        }

        // Thread count menu: assert current value is preselected, select it (no changes)
        decisions += { title, entries, startingIndex ->
            assertEquals("Thread count", title)
            val expectedIndex = entries.indexOf(expectedThreadCount.toString())
            assertTrue(expectedIndex >= 0, "Expected current thread count to be present in the list")
            assertEquals(expectedIndex, startingIndex, "Expected current thread count to be preselected by cursor")
            expectedThreadCount.toString()
        }

        // Back to App menu: assert we stayed on Thread count item
        decisions += { title, entries, startingIndex ->
            assertEquals("Application Settings", title)
            val threadIndex = entries.indexOfFirst { it.startsWith("Thread count:") }
            assertTrue(threadIndex >= 0, "Expected 'Thread count' entry to exist")
            assertEquals(threadIndex, startingIndex, "Expected to keep selection on 'Thread count' after returning from edit")

            val back = entries.firstOrNull { it == "Back" }
            assertNotNull(back)
            back
        }

        // Main menu -> Exit
        decisions += { title, entries, _ ->
            assertEquals("Settings", title)
            val exit = entries.firstOrNull { it == "Exit" }
            assertNotNull(exit)
            exit
        }

        // Exit confirmation: Exit
        decisions += { title, entries, startingIndex ->
            assertEquals("Exit?", title)
            assertEquals(listOf("Exit", "Cancel"), entries)
            assertEquals(entries.lastIndex, startingIndex)
            "Exit"
        }

        InteractiveSettingsMenu(
            prompter = FakePrompter(decisions),
            appSettings = appSettings,
            scanSettings = scanSettings,
            userSignatureSettings = userSignatureSettings,
        ).run()
    }

    @Test
    fun `matchers selection applies on exit without apply or cancel entries`() = runBlocking {
        val appSettings = AppSettings()
        val scanSettings = ScanSettings()
        val userSignatureSettings = UserSignatureSettings()

        val allMatchers = MatchersRegister.toList()
        assertTrue(allMatchers.size >= 2, "Expected at least two matchers to exist")

        val selectedIds = setOf(
            allMatchers[0].name.replace(" ", "_"),
            allMatchers[1].name.replace(" ", "_"),
        )

        val decisions = ArrayDeque<(String, List<String>, Int) -> String?>()

        // Main menu -> Scan Settings
        decisions += { title, entries, _ ->
            assertEquals("Settings", title)
            val scan = entries.firstOrNull { it.startsWith("Scan Settings") }
            assertNotNull(scan)
            scan
        }

        // Scan menu -> Matchers
        decisions += { title, entries, _ ->
            assertEquals("Scan Settings", title)
            val matchers = entries.firstOrNull { it.startsWith("Matchers:") }
            assertNotNull(matchers)
            matchers
        }

        // Scan menu -> Back
        decisions += { title, entries, _ ->
            assertEquals("Scan Settings", title)
            val back = entries.firstOrNull { it == "Back" }
            assertNotNull(back)
            back
        }

        // Main menu -> Exit
        decisions += { title, entries, _ ->
            assertEquals("Settings", title)
            val exit = entries.firstOrNull { it == "Exit" }
            assertNotNull(exit)
            exit
        }

        // Exit confirmation: Exit
        decisions += { title, entries, startingIndex ->
            assertEquals("Exit?", title)
            assertEquals(listOf("Save and exit", "Exit", "Cancel"), entries)
            assertEquals(entries.lastIndex, startingIndex)
            "Exit"
        }

        val prompter = FakePrompterWithMulti(
            decisions = decisions,
            multiDecision = { title, entries, initialSelected ->
                assertEquals("Matchers", title)
                assertTrue(entries.none { it.equals("Apply", ignoreCase = true) })
                assertTrue(entries.none { it.equals("Cancel", ignoreCase = true) })
                assertTrue(initialSelected.isNotEmpty(), "Expected scan settings to have some initial matchers")
                selectedIds
            },
        )

        InteractiveSettingsMenu(
            prompter = prompter,
            appSettings = appSettings,
            scanSettings = scanSettings,
            userSignatureSettings = userSignatureSettings,
        ).run()

        assertEquals(1, prompter.multiCalls, "Expected matchers multiSelect to be invoked once")
        val applied = scanSettings.matchers.map { it.name.replace(" ", "_") }.toSet()
        assertEquals(selectedIds, applied)
    }
}
