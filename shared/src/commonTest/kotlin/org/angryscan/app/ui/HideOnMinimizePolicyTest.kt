package org.angryscan.app.ui

import org.angryscan.app.common.OS
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HideOnMinimizePolicyTest {
    @Test
    fun hidesOnWindowsWhenSettingEnabled() {
        assertTrue(shouldHideOnMinimizeToTray(hideOnMinimize = true, os = OS.WINDOWS))
    }

    @Test
    fun doesNotHideOnLinuxEvenWhenSettingEnabled() {
        assertFalse(shouldHideOnMinimizeToTray(hideOnMinimize = true, os = OS.LINUX))
    }

    @Test
    fun doesNotHideWhenSettingDisabled() {
        assertFalse(shouldHideOnMinimizeToTray(hideOnMinimize = false, os = OS.WINDOWS))
        assertFalse(shouldHideOnMinimizeToTray(hideOnMinimize = false, os = OS.MAC))
        assertFalse(shouldHideOnMinimizeToTray(hideOnMinimize = false, os = OS.LINUX))
    }
}
