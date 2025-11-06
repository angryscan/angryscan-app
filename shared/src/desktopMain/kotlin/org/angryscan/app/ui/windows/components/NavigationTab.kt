package org.angryscan.app.ui.windows.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.angryscan.app.navigation.AppScreen

@Composable
fun NavigationTab(
    item: NavigationItem,
    onClick: () -> Unit
) {
    val (width, height) = when (item.route) {
        is AppScreen.Scans -> 240.dp to 60.dp
        else -> 180.dp to 60.dp
    }
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isHovered by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.92f
            isHovered -> 1.05f
            item.isSelected -> 1.08f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.8f
            isHovered -> 0.98f
            else -> 1f
        },
        animationSpec = tween(150, easing = EaseInOutCubic),
        label = "alpha"
    )
    
    val elevation by animateFloatAsState(
        targetValue = when {
            isPressed -> 1f
            isHovered -> 6f
            item.isSelected -> 8f
            else -> 0f
        },
        animationSpec = tween(200, easing = EaseInOutCubic),
        label = "elevation"
    )
    
    val rotation by animateFloatAsState(
        targetValue = when {
            isPressed -> 1f
            isHovered -> -0.5f
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .scale(scale)
            .alpha(alpha)
            .rotate(rotation)
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (item.isSelected) 
                    MaterialTheme.colorScheme.primary
                else if (isHovered)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else 
                    Color.Transparent
            )
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
            .clickable(
                enabled = true,
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    radius = 200.dp
                ),
                onClick = {
                    onClick()
                }
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                val iconScale by animateFloatAsState(
                    targetValue = when {
                        isPressed -> 0.95f
                        isHovered -> 1.05f
                        item.isSelected -> 1.08f
                        else -> 1f
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "iconScale"
                )
                
                val iconRotation by animateFloatAsState(
                    targetValue = when {
                        isPressed -> 2f
                        isHovered -> -1f
                        else -> 0f
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "iconRotation"
                )
                
                Icon(
                    painter = item.icon,
                    contentDescription = item.label,
                    modifier = Modifier
                        .size(18.dp)
                        .scale(iconScale)
                        .rotate(iconRotation),
                    tint = if (item.isSelected) 
                        MaterialTheme.colorScheme.onPrimary
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                if (item.hasActiveIndicator) {
                    ActiveScanIndicator()
                }
            }
            
            val textScale by animateFloatAsState(
                targetValue = when {
                    isPressed -> 0.98f
                    isHovered -> 1.02f
                    item.isSelected -> 1.05f
                    else -> 1f
                },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "textScale"
            )
            
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (item.isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (item.isSelected) 
                    MaterialTheme.colorScheme.onPrimary
                else 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.scale(textScale)
            )
        }
    }
}

