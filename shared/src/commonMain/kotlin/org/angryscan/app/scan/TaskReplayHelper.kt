package org.angryscan.app.scan

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object TaskReplayHelper {
    private val _pending = MutableStateFlow<TaskReplaySettings?>(null)
    val pending = _pending.asStateFlow()

    fun set(settings: TaskReplaySettings) {
        _pending.value = settings
    }

    fun clear() {
        _pending.value = null
    }
}
