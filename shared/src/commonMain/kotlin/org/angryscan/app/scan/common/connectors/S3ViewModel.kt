package org.angryscan.app.scan.common.connectors

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface S3ViewState {
    data class Success(val files: List<S3File>, val prefix: String) : S3ViewState
    data object Loading : S3ViewState
    data object Failure : S3ViewState
}

class S3ViewModel(
    val connector: ConnectorS3,
    dir: String = ""
) : ViewModel() {

    private val _state = MutableStateFlow<S3ViewState>(S3ViewState.Loading)
    val state = _state.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        coroutineScope.launch {
            val files = connector.getFiles(dir)
            _state.update { S3ViewState.Success(files, dir) }
        }
    }

    fun setDir(dir: String) {
        _state.update { S3ViewState.Loading }
        coroutineScope.launch {
            val files = connector.getFiles(dir)
            _state.update { S3ViewState.Success(files, dir) }
        }
    }
}