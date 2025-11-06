package org.angryscan.app.ui.windows.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
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
    backgroundColor: Color
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isHovered by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
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
            isPressed -> 2f
            isHovered -> 8f
            else -> 0f
        },
        animationSpec = tween(200, easing = EaseInOutCubic),
        label = "elevation"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .alpha(alpha)
            .rotate(rotation)
            .size(36.dp)
            .background(
                color = if (isHovered) 
                    backgroundColor.copy(alpha = 0.9f)
                else 
                    backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = backgroundColor.copy(alpha = 0.4f),
                spotColor = backgroundColor.copy(alpha = 0.5f)
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
            tint = if (isHovered)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f)
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
    }
}

