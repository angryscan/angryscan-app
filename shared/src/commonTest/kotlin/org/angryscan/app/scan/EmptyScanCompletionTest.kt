package org.angryscan.app.scan

import org.angryscan.app.db.models.TaskState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class EmptyScanCompletionTest {
    @Test
    fun `zero discovered files complete without entering SCANNING`() {
        assertEquals(TaskState.COMPLETED, TaskEntityViewModel.stateAfterFileDiscovery(0L))
        assertNotEquals(TaskState.SCANNING, TaskEntityViewModel.stateAfterFileDiscovery(0L))
    }

    @Test
    fun `discovered files proceed to SCANNING`() {
        assertEquals(TaskState.SCANNING, TaskEntityViewModel.stateAfterFileDiscovery(1L))
        assertEquals(TaskState.SCANNING, TaskEntityViewModel.stateAfterFileDiscovery(42L))
        assertNotEquals(TaskState.COMPLETED, TaskEntityViewModel.stateAfterFileDiscovery(1L))
    }

    @Test
    fun `negative discovered count is treated as empty and completes`() {
        // Defensive: callers should pass >= 0, but zero-or-less must not enter SCANNING.
        assertEquals(TaskState.COMPLETED, TaskEntityViewModel.stateAfterFileDiscovery(-1L))
    }
}
