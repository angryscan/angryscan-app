package org.angryscan.app.scan.common

import androidx.compose.ui.awt.ComposeWindow
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitMacOSSettings

lateinit var mainWindow: ComposeWindow

actual fun createDialogSettings(): FileKitDialogSettings {
    return FileKitDialogSettings(
        parentWindow = mainWindow,
        macOS = FileKitMacOSSettings(
            canCreateDirectories = true
        )
    )
}