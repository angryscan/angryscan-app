package org.angryscan.app.ui.windows.screens.settings.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import org.jetbrains.compose.resources.stringResource
import org.angryscan.app.resources.About_Description
import org.angryscan.app.resources.About_License
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.SideMenu_AboutPage
import org.angryscan.app.ui.dialogs.DescriptionDialog
import org.angryscan.app.ui.dialogs.LicenseDialog
import org.angryscan.app.ui.windows.screens.settings.SettingsRow
import org.angryscan.app.ui.windows.screens.settings.components.SettingsButton

@Composable
fun AboutSettings() {
    var showDescriptionDialog by remember { mutableStateOf(false) }
    val descriptionDialogState = rememberDialogState(width = 600.dp, height = 450.dp)

    var showLicenseDialog by remember { mutableStateOf(false) }
    val licenseDialogState = rememberDialogState(width = 600.dp, height = 580.dp)

    SettingsRow(title = stringResource(Res.string.SideMenu_AboutPage)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsButton(
                onClick = { showDescriptionDialog = true },
                text = stringResource(Res.string.About_Description)
            )

            SettingsButton(
                onClick = { showLicenseDialog = true },
                text = stringResource(Res.string.About_License)
            )
        }
    }

    if (showDescriptionDialog) {
        DescriptionDialog(
            onCloseRequest = { showDescriptionDialog = false },
            dialogState = descriptionDialogState
        )
    }

    if (showLicenseDialog) {
        LicenseDialog(
            onCloseRequest = { showLicenseDialog = false },
            dialogState = licenseDialogState
        )
    }
}

