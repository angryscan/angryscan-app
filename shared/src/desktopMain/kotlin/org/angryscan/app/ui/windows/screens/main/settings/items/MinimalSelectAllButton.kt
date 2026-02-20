package org.angryscan.app.ui.windows.screens.main.settings.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.ScanSettings_DiscardAll
import org.angryscan.app.resources.ScanSettings_SelectAll
import org.angryscan.common.engine.IMatcher
import org.jetbrains.compose.resources.stringResource

@Composable
fun MinimalSelectAllButton(
    scanSettings: ScanSettings,
    selectedCountry: MatcherCountry = MatcherCountry.ALL,
    availableMatchers: List<IMatcher>
) {
    val matchers = remember { scanSettings.matchers }
    
    // Получаем мэтчеры для текущей страны из доступных матчеров
    val countryMatchers = remember(selectedCountry, availableMatchers) {
        MatcherCountryMapping.filterMatchers(availableMatchers, selectedCountry)
    }
    
    val isAllSelected = countryMatchers.all { m ->
        matchers.any { m::class == it::class }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isAllSelected) {
                    // Удаляем только мэтчеры текущей страны
                    countryMatchers.forEach { matcher ->
                        scanSettings.matchers.removeAll { it::class == matcher::class }
                    }
                } else {
                    // Добавляем только мэтчеры текущей страны
                    scanSettings.matchers.addAll(countryMatchers.filter { m ->
                        !scanSettings.matchers.any { it::class == m::class }
                    })
                }
                scanSettings.save()
            },
        shape = RoundedCornerShape(8.dp),
        color = if (isHovered)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shadowElevation = if (isHovered) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isHovered)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                modifier = Modifier.size(24.dp)
            ) {
                Checkbox(
                    checked = isAllSelected,
                    onCheckedChange = { checked ->
                        if (checked) {
                            // Добавляем только мэтчеры текущей страны
                            scanSettings.matchers.addAll(countryMatchers.filter { m ->
                                !scanSettings.matchers.any { m::class == it::class }
                            })
                        } else {
                            // Удаляем только мэтчеры текущей страны
                            countryMatchers.forEach { matcher ->
                                scanSettings.matchers.removeAll { it::class == matcher::class }
                            }
                        }
                        scanSettings.save()
                    },
                    modifier = Modifier.size(18.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                )
            }

            Text(
                text = if (isAllSelected) stringResource(Res.string.ScanSettings_DiscardAll) else stringResource(Res.string.ScanSettings_SelectAll),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = when {
                    isAllSelected -> MaterialTheme.colorScheme.error
                    isHovered -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}