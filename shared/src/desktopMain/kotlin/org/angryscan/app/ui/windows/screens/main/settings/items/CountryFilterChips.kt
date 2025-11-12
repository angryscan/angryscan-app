package org.angryscan.app.ui.windows.screens.main.settings.items

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CountryFilterChips(
    selectedCountry: MatcherCountry,
    onCountrySelected: (MatcherCountry) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MatcherCountry.entries.forEach { country ->
            CountryChip(
                country = country,
                isSelected = country == selectedCountry,
                onClick = { onCountrySelected(country) }
            )
        }
    }
}

@Composable
private fun CountryChip(
    country: MatcherCountry,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        animationSpec = tween(200),
        label = "chip_background"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
            isHovered -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "chip_text"
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) 4.dp else if (isHovered) 2.dp else 0.dp,
        animationSpec = tween(200),
        label = "chip_elevation"
    )

    Surface(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .hoverable(interactionSource = interactionSource),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        shadowElevation = elevation
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = country.flag,
                fontSize = 16.sp
            )

            Text(
                text = country.getLocalizedName(),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@Composable
fun CompactCountryFilterChips(
    selectedCountry: MatcherCountry,
    onCountrySelected: (MatcherCountry) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MatcherCountry.entries.forEach { country ->
            CompactCountryChip(
                country = country,
                isSelected = country == selectedCountry,
                onClick = { onCountrySelected(country) }
            )
        }
    }
}

@Composable
private fun CompactCountryChip(
    country: MatcherCountry,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        animationSpec = tween(200),
        label = "compact_chip_background"
    )

    Surface(
        modifier = modifier
            .size(36.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .hoverable(interactionSource = interactionSource),
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        shadowElevation = if (isSelected) 3.dp else 0.dp
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = country.flag,
                fontSize = 20.sp
            )
        }
    }
}

