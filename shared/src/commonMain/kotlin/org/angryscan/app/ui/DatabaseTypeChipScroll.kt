package org.angryscan.app.ui

import androidx.compose.ui.geometry.Offset

/** Maps pointer wheel delta to horizontal scroll amount (px); combines X and inverted Y for desktop wheels. */
internal fun pointerScrollToHorizontalScrollPx(delta: Offset): Float = delta.x - delta.y
