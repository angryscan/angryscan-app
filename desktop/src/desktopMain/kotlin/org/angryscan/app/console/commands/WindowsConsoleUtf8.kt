package org.angryscan.app.console.commands

import org.angryscan.app.common.OS

/**
 * Windows console input/output encoding helper.
 *
 * Why:
 * - Mordant's JVM prompt reads input via Kotlin/JVM line reader.
 * - On Windows, when console code page is not UTF-8, reading non-ASCII input (e.g. Cyrillic) can
 *   throw MalformedInputException.
 *
 * Fix:
 * - Set console input/output code pages to UTF-8 (65001) for the current process.
 */
internal object WindowsConsoleUtf8 {
    private const val CP_UTF8 = 65001

    fun ensureUtf8() {
        if(OS.currentOS() != OS.WINDOWS) return

        try {
            // Prefer JNA platform if present on classpath (it is bundled for Mordant Windows).
            val kernel32Class = Class.forName("com.sun.jna.platform.win32.Kernel32")
            val instanceField = kernel32Class.getField("INSTANCE")
            val kernel32 = instanceField.get(null)

            val setConsoleCP = kernel32Class.getMethod("SetConsoleCP", Int::class.javaPrimitiveType)
            val setConsoleOutputCP = kernel32Class.getMethod("SetConsoleOutputCP", Int::class.javaPrimitiveType)

            setConsoleCP.invoke(kernel32, CP_UTF8)
            setConsoleOutputCP.invoke(kernel32, CP_UTF8)
        } catch (_: Throwable) {
            // If we can't change code pages, we still allow the app to continue.
        }
    }
}


