package org.angryscan.app.ui

import org.angryscan.app.common.OS
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopNotificationPolicyTest {
    @Test
    fun enablesNativeNotificationOnWindows() {
        assertTrue(
            shouldUseNativeScanCompletionNotification(
                os = OS.WINDOWS,
                javaHome = "C:/Program Files/Java/jdk-21",
                jpackageAppPath = null
            )
        )
    }

    @Test
    fun enablesNativeNotificationOnMacWhenStartedFromJpackage() {
        assertTrue(
            shouldUseNativeScanCompletionNotification(
                os = OS.MAC,
                javaHome = "/Users/test/Library/Java/JavaVirtualMachines/temurin-21/Contents/Home",
                jpackageAppPath = "/Applications/AngryScanner.app/Contents/MacOS/AngryScanner"
            )
        )
    }

    @Test
    fun disablesNativeNotificationOnMacWhenStartedFromLocalJdk() {
        assertFalse(
            shouldUseNativeScanCompletionNotification(
                os = OS.MAC,
                javaHome = "/Users/test/Library/Java/JavaVirtualMachines/temurin-21/Contents/Home",
                jpackageAppPath = null
            )
        )
    }

    @Test
    fun enablesNativeNotificationOnMacWhenRuntimeIsInsideAppBundle() {
        assertTrue(
            shouldUseNativeScanCompletionNotification(
                os = OS.MAC,
                javaHome = "/Applications/AngryScanner.app/Contents/runtime/Contents/Home",
                jpackageAppPath = null
            )
        )
    }
}
