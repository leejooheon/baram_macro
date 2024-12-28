package common.base

import common.model.Type
import common.model.UiState
import follower.model.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object UiStateHolder {
    private val _state = MutableStateFlow(UiState.default)
    internal val state = _state.asStateFlow()

    suspend fun init(uiState: UiState) {
        _state.emit(uiState)
    }

    fun update(
        isRunning: Boolean,
        connectionState: ConnectionState
    ) {
        _state.update {
            it.copy(
                isRunning = isRunning,
                connectionState = connectionState
            )
        }
    }

    fun update(
        type: Type,
        state: UiState.CommonState
    ) {
        _state.update {
            when(type) {
                Type.X -> it.copy(xState =  state)
                Type.Y -> it.copy(yState = state)
                Type.BUFF -> it.copy(buffState = state)
                Type.MAGIC_RESULT -> it.copy(magicResultState = state)
            }
        }
    }
}