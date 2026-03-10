package org.angryscan.app.scan

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class TasksViewModel : ViewModel() {
    private val _tasks: MutableStateFlow<List<TaskEntityViewModel>> = MutableStateFlow(emptyList())
    val tasks = _tasks.asStateFlow()
    private val completedTasksGuard = mutableSetOf<Int>()
    private val _completedTaskIds = MutableSharedFlow<Int>(extraBufferCapacity = 64)
    val completedTaskIds = _completedTaskIds.asSharedFlow()

    fun add(task: TaskEntityViewModel) {
        _tasks.value += task
    }

    fun delete(task: TaskEntityViewModel) {
        _tasks.value -= task
        task.id.value?.let { completedTasksGuard.remove(it) }
    }

    fun notifyTaskCompleted(taskId: Int?) {
        if (taskId == null) return

        val shouldNotify = synchronized(completedTasksGuard) {
            completedTasksGuard.add(taskId)
        }
        if (shouldNotify) {
            _completedTaskIds.tryEmit(taskId)
        }
    }

    fun resetTaskCompletionNotification(taskId: Int?) {
        if (taskId == null) return
        synchronized(completedTasksGuard) {
            completedTasksGuard.remove(taskId)
        }
    }

    fun setAll(list: List<TaskEntityViewModel>) {
        _tasks.value = list
    }
}