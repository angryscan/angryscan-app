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
import org.angryscan.app.ui.windows.screens.main.LocalMainScreenAdaptiveTokens

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
    val adaptiveScale = LocalMainScreenAdaptiveTokens.current.scale
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(adaptiveScale.dp(12.dp, min = 10.dp, max = 18.dp)),
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
    val adaptiveScale = LocalMainScreenAdaptiveTokens.current.scale
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
            shape = RoundedCornerShape(adaptiveScale.dp(20.dp, min = 16.dp, max = 28.dp)),
            color = fill,
            border = BorderStroke(adaptiveScale.dp(1.dp, min = 1.dp, max = 1.4.dp), stroke),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .padding(
                        horizontal = adaptiveScale.dp(12.dp, min = 10.dp, max = 18.dp),
                        vertical = adaptiveScale.dp(8.dp, min = 6.dp, max = 12.dp)
                    ),
                horizontalArrangement = Arrangement.spacedBy(adaptiveScale.dp(6.dp, min = 5.dp, max = 10.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = country.flag,
                    fontSize = adaptiveScale.sp(20.sp, min = 18.sp, max = 24.sp)
                )

                if (selectedCount != null && totalCount != null) {
                    Text(
                        text = "$selectedCount/$totalCount",
                        fontSize = adaptiveScale.sp(10.sp, min = 9.sp, max = 12.sp),
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
    val adaptiveScale = LocalMainScreenAdaptiveTokens.current.scale
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            horizontalSpacing ?: if (dense) adaptiveScale.dp(8.dp, min = 7.dp, max = 12.dp) else adaptiveScale.dp(10.dp, min = 8.dp, max = 16.dp)
        ),
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
    val adaptiveScale = LocalMainScreenAdaptiveTokens.current.scale
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
            shape = RoundedCornerShape(
                if (dense) adaptiveScale.dp(8.dp, min = 7.dp, max = 12.dp) else adaptiveScale.dp(10.dp, min = 9.dp, max = 14.dp)
            ),
            color = fill,
            border = BorderStroke(adaptiveScale.dp(1.dp, min = 1.dp, max = 1.4.dp), stroke),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (dense) adaptiveScale.dp(5.dp, min = 4.dp, max = 8.dp) else adaptiveScale.dp(8.dp, min = 7.dp, max = 12.dp),
                    vertical = if (dense) adaptiveScale.dp(2.dp, min = 2.dp, max = 4.dp) else adaptiveScale.dp(4.dp, min = 3.dp, max = 6.dp)
                ),
                horizontalArrangement = Arrangement.spacedBy(
                    if (dense) adaptiveScale.dp(3.dp, min = 2.dp, max = 5.dp) else adaptiveScale.dp(6.dp, min = 5.dp, max = 10.dp)
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = country.flag,
                    fontSize = if (dense) adaptiveScale.sp(13.sp, min = 12.sp, max = 16.sp) else adaptiveScale.sp(20.sp, min = 18.sp, max = 24.sp)
                )
                if (selectedCount != null && totalCount != null) {
                    Text(
                        text = "$selectedCount/$totalCount",
                        fontSize = if (dense) adaptiveScale.sp(8.sp, min = 8.sp, max = 10.sp) else adaptiveScale.sp(9.sp, min = 9.sp, max = 11.sp),
                        fontWeight = FontWeight.Medium,
                        color = labelColor
                    )
                }
            }
        }
    }
}
