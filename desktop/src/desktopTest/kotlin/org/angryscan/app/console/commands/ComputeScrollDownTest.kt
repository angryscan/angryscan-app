package org.angryscan.app.console.commands

import kotlin.test.Test
import kotlin.test.assertEquals

internal class ComputeScrollDownTest {
    @Test
    fun `keeps cursor visible without scrolling`() {
        // viewport shows indices 0..2
        assertEquals(0, computeScrollDown(cursorIndex = 0, currentScrollDown = 0, viewportHeight = 3, itemCount = 10))
        assertEquals(0, computeScrollDown(cursorIndex = 2, currentScrollDown = 0, viewportHeight = 3, itemCount = 10))
    }

    @Test
    fun `scrolls down when cursor goes below viewport`() {
        // viewport height 3, cursor at 3 should scroll so that 3 is last visible => scrollDown = 1
        assertEquals(1, computeScrollDown(cursorIndex = 3, currentScrollDown = 0, viewportHeight = 3, itemCount = 10))
        // cursor at 5 => scrollDown = 3
        assertEquals(3, computeScrollDown(cursorIndex = 5, currentScrollDown = 0, viewportHeight = 3, itemCount = 10))
    }

    @Test
    fun `scrolls up when cursor goes above viewport`() {
        // start scrolled down to 5 (shows 5..7), cursor moves to 4 => scrollDown should become 4
        assertEquals(4, computeScrollDown(cursorIndex = 4, currentScrollDown = 5, viewportHeight = 3, itemCount = 10))
    }

    @Test
    fun `clamps scroll to valid range`() {
        // itemCount smaller than viewport => scroll always 0
        assertEquals(0, computeScrollDown(cursorIndex = 0, currentScrollDown = 100, viewportHeight = 10, itemCount = 3))
        // cursor beyond last => clamped
        assertEquals(0, computeScrollDown(cursorIndex = 999, currentScrollDown = 0, viewportHeight = 10, itemCount = 3))
    }
}
