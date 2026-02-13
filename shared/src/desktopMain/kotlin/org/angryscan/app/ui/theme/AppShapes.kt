package org.angryscan.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

private val ExtraSmall = RoundedCornerShape(8.dp)
private val Small = RoundedCornerShape(12.dp)
private val Medium = RoundedCornerShape(16.dp)
private val Large = RoundedCornerShape(24.dp)
private val ExtraLarge = RoundedCornerShape(28.dp)

val AppShapes = Shapes(
    extraSmall = ExtraSmall,
    small = Small,
    medium = Medium,
    large = Large,
    extraLarge = ExtraLarge
)