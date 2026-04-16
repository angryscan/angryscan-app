package org.angryscan.app.ui.tray

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ApplicationScope
import com.kdroid.composetray.tray.api.Tray
import org.jetbrains.compose.resources.stringResource
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.appName
import org.angryscan.app.resources.icon
import org.angryscan.app.resources.trayExit
import org.angryscan.app.resources.trayHide
import org.angryscan.app.resources.trayOpen

@Composable
fun ApplicationScope.AppTray(
    mainIsVisible: Boolean,
    mainVisibilitySet: (Boolean) -> Unit,
) {
    val openLabel = stringResource(Res.string.trayOpen)
    val hideLabel = stringResource(Res.string.trayHide)
    val exitLabel = stringResource(Res.string.trayExit)
    val trayLabel = if (mainIsVisible) hideLabel else openLabel
    val trayActionIcon = if (mainIsVisible) Icons.Default.VisibilityOff else Icons.AutoMirrored.Filled.OpenInNew

    Tray(
        icon = Res.drawable.icon,
        tooltip = stringResource(Res.string.appName),
        primaryAction = {
            mainVisibilitySet(true)
        }
    ) {
        Item(
            label = trayLabel,
            icon = trayActionIcon,
            iconTint = null
        ) {
            mainVisibilitySet(!mainIsVisible)
        }
        Divider()
        Item(
            label = exitLabel,
            icon = Icons.Default.PowerSettingsNew,
            iconTint = null
        ) {
            dispose()
            exitApplication()
        }
    }
}
