package org.angryscan.app.scan.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ScanPathHelper {
    private val _path = MutableStateFlow("")
    val path = _path.asStateFlow()

    private val _focusRequested = MutableStateFlow(false)
    val focusRequested = _focusRequested.asStateFlow()

    fun setPath(path: String) {
        _path.value = path
    }

    fun requestFocus() {
        _focusRequested.value = true
    }

    fun resetFocus() {
        _focusRequested.value = false
    }
}