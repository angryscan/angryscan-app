package org.angryscan.app.ui

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseTypeChipScrollTest {

    @Test
    fun verticalWheelMapsToHorizontalSigned() {
        assertEquals(10f, pointerScrollToHorizontalScrollPx(Offset(0f, -10f)))
        assertEquals(-10f, pointerScrollToHorizontalScrollPx(Offset(0f, 10f)))
    }

    @Test
    fun horizontalWheelAddsDirectly() {
        assertEquals(5f, pointerScrollToHorizontalScrollPx(Offset(5f, 0f)))
    }
}
