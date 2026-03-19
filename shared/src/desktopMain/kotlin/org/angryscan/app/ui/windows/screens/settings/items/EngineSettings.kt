package org.angryscan.app.ui.windows.screens.settings.items

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.SettingsScreen_ScanEngine
import org.angryscan.app.resources.SettingsScreen_ScanEngineDescription
import org.angryscan.app.ui.strings.composableName
import org.angryscan.app.ui.windows.screens.settings.SettingsRow
import org.angryscan.app.ui.windows.screens.settings.components.SettingsSelector
import org.angryscan.common.engine.IScanEngine
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.reflect.KClass

@Composable
fun EngineSettings(modifier: Modifier = Modifier) {
    val scanSettings = koinInject<ScanSettings>()
    var engine by remember { scanSettings.engine }

    SettingsRow(
        title = stringResource(Res.string.SettingsScreen_ScanEngine),
        modifier = modifier
    ) {
        EngineSettingsContent(
            engine = engine,
            onEngineSelect = { eng ->
                engine = eng
                scanSettings.save()
            }
        )
    }
}

@Composable
fun EngineSettingsContent(
    engine: KClass<out IScanEngine>,
    onEngineSelect: (KClass<out IScanEngine>) -> Unit,
    showDescription: Boolean = true,
) {
    val engines: List<KClass<out IScanEngine>> = listOf(HyperScanEngine::class, KotlinEngine::class)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showDescription) {
            Text(
                text = stringResource(Res.string.SettingsScreen_ScanEngineDescription),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val minCellWidth = 180.dp
            val columns = (maxWidth / minCellWidth).toInt().coerceAtLeast(1)
            val rows = (engines.size + columns - 1) / columns
            val height = (34 * rows + (8 * (rows - 1).coerceAtLeast(0))).dp
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .height(height)
                    .fillMaxWidth()
            ) {
                items(engines) { eng ->
                    SettingsSelector(
                        selected = eng == engine,
                        onClick = { onEngineSelect(eng) },
                        text = eng.composableName()
                    )
                }
            }
        }
    }
}
