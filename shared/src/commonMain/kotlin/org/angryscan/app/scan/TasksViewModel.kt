package org.angryscan.app.scan

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TasksViewModel : ViewModel() {
    private val _tasks: MutableStateFlow<List<TaskEntityViewModel>> = MutableStateFlow(emptyList())
    val tasks = _tasks.asStateFlow()

    fun add(task: TaskEntityViewModel) {
        _tasks.value += task
    }

    fun delete(task: TaskEntityViewModel) {
        _tasks.value -= task
    }
}