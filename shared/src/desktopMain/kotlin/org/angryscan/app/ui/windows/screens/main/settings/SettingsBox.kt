package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Transition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.angryscan.app.common.ScanSettings
import org.koin.compose.koinInject

@Composable
fun SettingsBox(
    transition: Transition<Boolean>,
) {
    val scanSettings = koinInject<ScanSettings>()

    AnimatedVisibility(
        visible = transition.currentState,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        val subsectionDividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f)
        SettingsScanUnifiedPanel(modifier = Modifier.fillMaxSize()) {
            SettingsBoxExtensionsSelection(
                scanSettings,
                showTitle = true,
                unifiedBlock = true,
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                color = subsectionDividerColor
            )
            SettingsBoxDetectFunctionsGrouped(
                scanSettings,
                showTitle = true,
                unifiedBlock = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .fillMaxHeight()
            )
            /*
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                color = subsectionDividerColor
            )
            SettingsBoxUserSignature(
                scanSettings,
                showTitle = true,
                unifiedBlock = true,
                modifier = Modifier.fillMaxWidth()
            )
            */
        }
    }
}
