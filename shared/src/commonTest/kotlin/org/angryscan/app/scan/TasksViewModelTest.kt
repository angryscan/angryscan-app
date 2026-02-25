package org.angryscan.app.scan

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class TasksViewModelTest {
    @Test
    fun emitsCompletedTaskEventOnlyOnceUntilReset() = runBlocking {
        val viewModel = TasksViewModel()
        val events = mutableListOf<Int>()

        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.completedTaskIds.collect {
                events += it
            }
        }

        viewModel.notifyTaskCompleted(42)
        viewModel.notifyTaskCompleted(42)
        delay(50)

        assertEquals(listOf(42), events)
        collector.cancelAndJoin()
    }

    @Test
    fun emitsCompletedTaskEventAgainAfterReset() = runBlocking {
        val viewModel = TasksViewModel()
        val events = mutableListOf<Int>()

        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.completedTaskIds.collect {
                events += it
            }
        }

        viewModel.notifyTaskCompleted(7)
        viewModel.resetTaskCompletionNotification(7)
        viewModel.notifyTaskCompleted(7)
        delay(50)

        assertEquals(listOf(7, 7), events)
        collector.cancelAndJoin()
    }
}
