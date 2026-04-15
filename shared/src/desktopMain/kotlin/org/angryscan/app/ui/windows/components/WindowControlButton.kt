package org.angryscan.app.ui.windows.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun WindowControlButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    /** Used for drop-shadow tint on dark theme; minimize/maximize vs close. */
    isCloseAction: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val cs = MaterialTheme.colorScheme
    /** Light top bar: no stacked drop-shadow on controls (avoids "box in box" with the bar). */
    val useFlatWindowControls = cs.surface.red + cs.surface.green + cs.surface.blue > 1.4f

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.9f
            isHovered -> 1.05f
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
    
    val rotation by animateFloatAsState(
        targetValue = when {
            isPressed -> 2f
            isHovered -> -1f
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rotation"
    )
    
    val elevation by animateFloatAsState(
        targetValue = when {
            isPressed -> if (useFlatWindowControls) 0f else 2f
            isHovered -> if (useFlatWindowControls) 0f else 8f
            else -> 0f
        },
        animationSpec = tween(200, easing = EaseInOutCubic),
        label = "elevation"
    )

    val shape = RoundedCornerShape(8.dp)
    val shadowTint = if (isCloseAction) cs.error else cs.surfaceVariant
    // Light top bar: use solid `error` (vivid); dark theme keeps `errorContainer` like before.
    val fillColor = when {
        !isCloseAction -> Color.Transparent
        isCloseAction && useFlatWindowControls -> when {
            isHovered -> Color(0xFFB91C1C)
            else -> cs.error
        }
        isCloseAction && isHovered -> cs.error.copy(alpha = 0.88f)
        else -> cs.errorContainer
    }
    Box(
        modifier = Modifier
            .scale(scale)
            .alpha(alpha)
            .rotate(rotation)
            .size(36.dp)
            .background(color = fillColor, shape = shape)
            .hoverable(interactionSource = interactionSource)
            .then(
                if (elevation > 0f) {
                    Modifier.shadow(
                        elevation = elevation.dp,
                        shape = shape,
                        ambientColor = shadowTint.copy(alpha = 0.4f),
                        spotColor = shadowTint.copy(alpha = 0.5f)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    radius = 50.dp
                ),
                onClick = {
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        val iconScale by animateFloatAsState(
            targetValue = if (isPressed) 0.85f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            ),
            label = "iconScale"
        )
        
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(16.dp)
                .scale(iconScale),
            tint = when {
                isCloseAction && useFlatWindowControls ->
                    cs.onError.copy(alpha = if (isHovered) 1f else 0.95f)
                isCloseAction ->
                    cs.onErrorContainer.copy(alpha = if (isHovered) 1f else 0.95f)
                isHovered -> cs.onSurface.copy(alpha = 0.95f)
                else -> cs.onSurface.copy(alpha = 0.85f)
            }
        )
    }
}

