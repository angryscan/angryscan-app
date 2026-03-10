package org.angryscan.app.ui.windows.screens.settings.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import org.angryscan.app.common.AppVersion
import org.angryscan.app.resources.*
import org.angryscan.app.ui.dialogs.ContactDialog
import org.angryscan.app.ui.dialogs.LicenseDialog
import org.angryscan.app.ui.windows.screens.settings.SettingsRow
import org.angryscan.app.ui.windows.screens.settings.components.SettingsButton
import org.jetbrains.compose.resources.stringResource

@Composable
fun AboutSettings(modifier: Modifier = Modifier) {
    var showContactDialog by remember { mutableStateOf(false) }
    val contactDialogState = rememberDialogState(width = 400.dp, height = 260.dp)

    var showLicenseDialog by remember { mutableStateOf(false) }
    val licenseDialogState = rememberDialogState(width = 600.dp, height = 590.dp)

    SettingsRow(title = stringResource(Res.string.SideMenu_AboutPage), modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = stringResource(Res.string.AboutScreen_Description),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(Res.string.AboutScreen_Version, AppVersion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsButton(
                    onClick = { showContactDialog = true },
                    text = stringResource(Res.string.ContactDialog_Title)
                )

                SettingsButton(
                    onClick = { showLicenseDialog = true },
                    text = stringResource(Res.string.LicenseDialog_Title)
                )
            }
        }
    }

    if (showContactDialog) {
        ContactDialog(
            onCloseRequest = { showContactDialog = false },
            dialogState = contactDialogState
        )
    }

    if (showLicenseDialog) {
        LicenseDialog(
            onCloseRequest = { showLicenseDialog = false },
            dialogState = licenseDialogState
        )
    }
}

