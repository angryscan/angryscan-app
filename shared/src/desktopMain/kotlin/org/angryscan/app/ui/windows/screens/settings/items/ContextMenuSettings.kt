package org.angryscan.app.ui.windows.screens.settings.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.SettingsScreen_ContextMenu
import org.angryscan.app.resources.SettingsScreen_ContextMenuExplorer
import org.angryscan.app.store.ContextMenu
import org.angryscan.app.ui.windows.screens.settings.SettingsRow

@Composable
fun ContextMenuSettings() {
    var contextMenuEnabled by remember { mutableStateOf(ContextMenu.enabled) }

    if (ContextMenu.supported()) {
        SettingsRow(title = stringResource(Res.string.SettingsScreen_ContextMenu)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(Res.string.SettingsScreen_ContextMenuExplorer))

                Switch(
                    checked = contextMenuEnabled,
                    onCheckedChange = {
                        contextMenuEnabled = it
                        ContextMenu.enabled = it
                    }
                )
            }
        }
    }
}

