package org.angryscan.app.ui.windows.screens.settings.items

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.angryscan.app.common.AppFiles
import org.angryscan.app.common.AppSettings
import org.angryscan.app.resources.*
import org.angryscan.app.ui.windows.screens.settings.SettingsRow
import org.angryscan.app.ui.windows.screens.settings.components.SettingsButton
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import java.awt.Desktop

@Composable
fun LoggingSettings(modifier: Modifier = Modifier) {
    val appSettings = koinInject<AppSettings>()
    var debugModeEnabled by remember { appSettings.debugMode }

    SettingsRow(
        title = stringResource(Res.string.SettingsScreen_Logging),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = stringResource(Res.string.SettingsScreen_LoggingDescription),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.height(34.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.ScanSettings_DebugMode),
                    style = MaterialTheme.typography.bodyMedium
                )

                Switch(
                    checked = debugModeEnabled,
                    onCheckedChange = {
                        debugModeEnabled = it
                        appSettings.save()
                    }
                )
            }

            SettingsButton(
                onClick = {
                    Desktop.getDesktop().open(AppFiles.LoggingDir.toFile())
                },
                text = stringResource(Res.string.SettingsScreen_OpenFolder)
            )
            }
        }
    }
}

