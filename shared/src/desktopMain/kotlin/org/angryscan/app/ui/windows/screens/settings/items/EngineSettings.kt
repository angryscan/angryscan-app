package org.angryscan.app.ui.windows.screens.settings.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.angryscan.common.engine.IScanEngine
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.SettingsScreen_ScanEngine
import org.angryscan.app.ui.strings.composableName
import org.angryscan.app.ui.windows.screens.settings.SettingsRow
import org.angryscan.app.ui.windows.screens.settings.components.SettingsSelector
import kotlin.reflect.KClass

@Composable
fun EngineSettings() {
    val scanSettings = koinInject<ScanSettings>()
    var engine by remember { scanSettings.engine }

    SettingsRow(
        title = stringResource(Res.string.SettingsScreen_ScanEngine)) {
        val engines: List<KClass<out IScanEngine>> = listOf(HyperScanEngine::class, KotlinEngine::class)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .height(34.dp)
                .fillMaxWidth()
        ) {
            items(engines) { eng ->
                SettingsSelector(
                    selected = eng == engine,
                    onClick = {
                        engine = eng
                        scanSettings.save()
                    },
                    text = eng.composableName()
                )
            }
        }
    }
}
