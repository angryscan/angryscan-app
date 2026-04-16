package org.angryscan.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScanCompletionNotificationAssetsTest {
    @Test
    fun containsExpectedComposeResourcePaths() {
        val assets = scanCompletionNotificationAssets()

        assertEquals("drawable/icon.png", assets.appIconResourcePath)
        assertTrue(assets.completionMiniIconResourcePath.endsWith(".svg"))
    }
}
