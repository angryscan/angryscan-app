package org.angryscan.app.ui.windows.screens.settings.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.angryscan.app.common.AppSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.SettingsScreen_RecommendedThreads
import org.angryscan.app.resources.SettingsScreen_ThreadsCount
import org.angryscan.app.scan.ScanService
import org.angryscan.app.ui.windows.screens.settings.SettingsRow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private val valueBadgeShape = RoundedCornerShape(8.dp)

@Composable
fun ThreadCountSettings(modifier: Modifier = Modifier) {
    val appSettings = koinInject<AppSettings>()
    val scanService = koinInject<ScanService>()
    val colorScheme = MaterialTheme.colorScheme

    var sliderPosition by remember { mutableStateOf(appSettings.threadCount.value.toFloat()) }
    var threadCount by remember { appSettings.threadCount }
    val maxThreads = Runtime.getRuntime().availableProcessors()
    val recommendedThreads = (maxThreads + 1) / 2
    SettingsRow(title = stringResource(Res.string.SettingsScreen_ThreadsCount), modifier = modifier) {
        ThreadCountSettingsContent(
            sliderPosition = sliderPosition,
            maxThreads = maxThreads,
            recommendedThreads = recommendedThreads,
            colorScheme = colorScheme,
            onSliderChange = { sliderPosition = it },
            onSliderFinish = {
                threadCount = sliderPosition.toInt()
                appSettings.save()
                scanService.setThreadsCount()
            }
        )
    }
}

@Composable
fun ThreadCountSettingsContent(
    sliderPosition: Float,
    maxThreads: Int,
    recommendedThreads: Int,
    colorScheme: androidx.compose.material3.ColorScheme,
    onSliderChange: (Float) -> Unit,
    onSliderFinish: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Slider(
                value = sliderPosition,
                onValueChange = onSliderChange,
                valueRange = 1f..maxThreads.toFloat(),
                steps = (maxThreads - 2).coerceAtLeast(0),
                onValueChangeFinished = onSliderFinish,
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 500.dp)
                    .height(48.dp),
                colors = SliderDefaults.colors(
                    thumbColor = colorScheme.primary,
                    activeTrackColor = colorScheme.primary,
                    inactiveTrackColor = colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    activeTickColor = colorScheme.onPrimary.copy(alpha = 0.4f),
                    inactiveTickColor = colorScheme.outline.copy(alpha = 0.25f)
                )
            )
            Box(
                modifier = Modifier
                    .widthIn(min = 40.dp)
                    .height(30.dp)
                    .clip(valueBadgeShape)
                    .background(colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sliderPosition.toInt().toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
        Text(
            text = stringResource(Res.string.SettingsScreen_RecommendedThreads, recommendedThreads),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
        )
    }
}

