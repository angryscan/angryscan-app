package org.angryscan.app.ui.windows.screens.settings.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.angryscan.app.common.AppFiles
import org.angryscan.app.common.AppSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.ScanSettings_DebugMode
import org.angryscan.app.resources.SettingsScreen_Logging
import org.angryscan.app.resources.SettingsScreen_OpenFolder
import org.angryscan.app.ui.windows.screens.settings.SettingsRow
import org.angryscan.app.ui.windows.screens.settings.components.SettingsButton
import java.awt.Desktop

@Composable
fun LoggingSettings() {
    val appSettings = koinInject<AppSettings>()
    var debugModeEnabled by remember { appSettings.debugMode }

    SettingsRow(
        title = stringResource(Res.string.SettingsScreen_Logging)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(Res.string.ScanSettings_DebugMode))

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
//            OutlinedButton(
//                modifier = Modifier
//                    .size(width = 150.dp, height = 34.dp),
//                shape = MaterialTheme.shapes.large,
//                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary),
//                onClick = {
//                    Desktop.getDesktop().open(AppFiles.LoggingDir.toFile())
//                },
//                colors = ButtonDefaults.outlinedButtonColors().copy(
//                    contentColor = MaterialTheme.colorScheme.onPrimary
//                )
//            ) {
//                Text(
//                    text = stringResource(Res.string.SettingsScreen_OpenFolder),
//                    fontSize = 14.sp,
//                    lineHeight = 14.sp,
//                    fontWeight = MaterialTheme.typography.bodyMedium.fontWeight,
//                    textAlign = TextAlign.Center
//                )
//            }
        }
    }
}

