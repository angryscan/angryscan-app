package org.angryscan.app.ui.windows.screens.main.settings.items

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.angryscan.app.ui.windows.components.DescriptionTooltip

/** Тон-заливка и обводка как у QuickFilterChip (TopNavigation). */
@Composable
private fun scanFilterChipStyle(
    selected: Boolean,
    hovered: Boolean,
): Triple<Color, Color, Color> {
    val cs = MaterialTheme.colorScheme
    val fill = when {
        selected && hovered -> cs.primary.copy(alpha = 0.20f)
        selected -> cs.primary.copy(alpha = 0.16f)
        hovered -> cs.primary.copy(alpha = 0.10f)
        else -> cs.surfaceVariant.copy(alpha = 0.24f)
    }
    val stroke = when {
        selected -> cs.primary.copy(alpha = 0.28f)
        hovered -> cs.primary.copy(alpha = 0.28f)
        else -> cs.outlineVariant.copy(alpha = 0.26f)
    }
    val label = when {
        selected || hovered -> cs.primary
        else -> cs.onSurfaceVariant
    }
    return Triple(fill, stroke, label)
}

@Composable
fun CountryFilterChips(
    selectedCountry: MatcherCountry,
    onCountrySelected: (MatcherCountry) -> Unit,
    modifier: Modifier = Modifier,
    getCountryStats: ((MatcherCountry) -> Pair<Int, Int>)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MatcherCountry.entries.forEach { country ->
            val stats = getCountryStats?.invoke(country)
            CountryChip(
                country = country,
                isSelected = country == selectedCountry,
                onClick = { onCountrySelected(country) },
                selectedCount = stats?.first,
                totalCount = stats?.second
            )
        }
    }
}

@Composable
private fun CountryChip(
    country: MatcherCountry,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedCount: Int? = null,
    totalCount: Int? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val (fill, stroke, labelColor) = scanFilterChipStyle(isSelected, isHovered)

    DescriptionTooltip(
        description = country.getLocalizedName(useShort = false),
        delay = 300
    ) {
        Surface(
            modifier = modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() }
                .hoverable(interactionSource = interactionSource),
            shape = RoundedCornerShape(20.dp),
            color = fill,
            border = BorderStroke(1.dp, stroke),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = country.flag,
                    fontSize = 20.sp
                )

                if (selectedCount != null && totalCount != null) {
                    Text(
                        text = "$selectedCount/$totalCount",
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = labelColor
                    )
                }
            }
        }
    }
}

@Composable
fun CompactCountryFilterChips(
    selectedCountry: MatcherCountry?,
    onCountrySelected: (MatcherCountry?) -> Unit,
    modifier: Modifier = Modifier,
    getCountryStats: ((MatcherCountry) -> Pair<Int, Int>)? = null,
    dense: Boolean = false,
    horizontalSpacing: Dp? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing ?: (if (dense) 8.dp else 10.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MatcherCountry.entries
            .filter { it != MatcherCountry.ALL }
            .forEach { country ->
            val stats = getCountryStats?.invoke(country)
            CompactCountryChip(
                country = country,
                isSelected = country == selectedCountry,
                onClick = {
                    if (country != selectedCountry) {
                        onCountrySelected(country)
                    }
                },
                selectedCount = stats?.first,
                totalCount = stats?.second,
                dense = dense,
            )
        }
    }
}

@Composable
private fun CompactCountryChip(
    country: MatcherCountry,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedCount: Int? = null,
    totalCount: Int? = null,
    dense: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val (fill, stroke, labelColor) = scanFilterChipStyle(isSelected, isHovered)

    DescriptionTooltip(
        description = country.getLocalizedName(useShort = false),
        delay = 300
    ) {
        Surface(
            modifier = modifier
                .defaultMinSize(minHeight = if (dense) 22.dp else 36.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() }
                .hoverable(interactionSource = interactionSource),
            shape = RoundedCornerShape(if (dense) 8.dp else 10.dp),
            color = fill,
            border = BorderStroke(1.dp, stroke),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (dense) 5.dp else 8.dp,
                    vertical = if (dense) 2.dp else 4.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(if (dense) 3.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = country.flag,
                    fontSize = if (dense) 13.sp else 20.sp
                )
                if (selectedCount != null && totalCount != null) {
                    Text(
                        text = "$selectedCount/$totalCount",
                        fontSize = if (dense) 8.sp else 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = labelColor
                    )
                }
            }
        }
    }
}
