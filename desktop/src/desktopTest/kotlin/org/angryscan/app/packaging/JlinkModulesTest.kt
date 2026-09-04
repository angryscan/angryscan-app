package org.angryscan.app.packaging

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

/**
 * Compose Desktop packs a minimal JRE via jlink. Libraries that use JDK APIs
 * outside the default module set fail at runtime in packaged builds only.
 *
 * PostgreSQL JDBC (PGPropertyMaxResultBufferParser) loads
 * java.lang.management.ManagementFactory, which lives in java.management.
 */
class JlinkModulesTest {
    @Test
    fun nativeDistributionsIncludesModulesRequiredByPostgresqlJdbc() {
        val buildFile = locateDesktopBuildGradle()
        val content = buildFile.readText()

        val modulesBlock = Regex(
            """nativeDistributions\s*\{.*?modules\(([^)]+)\)""",
            RegexOption.DOT_MATCHES_ALL
        ).find(content)
            ?: error("Could not find nativeDistributions.modules(...) in ${buildFile.path}")

        val declaredModules = modulesBlock.groupValues[1]
            .split(',')
            .map { it.trim().trim('"') }
            .filter { it.isNotEmpty() }

        assertContains(
            declaredModules,
            "java.management",
            "Packaged runtime must include java.management so postgresql JDBC can use ManagementFactory"
        )
        assertTrue(
            declaredModules.contains("java.sql"),
            "Packaged runtime must include java.sql for JDBC drivers"
        )
    }

    private fun locateDesktopBuildGradle(): File {
        val candidates = listOf(
            File("build.gradle.kts"),
            File("desktop/build.gradle.kts"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("desktop/build.gradle.kts not found from ${File(".").absolutePath}")
    }
}
