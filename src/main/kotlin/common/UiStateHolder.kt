package common

import common.model.UiState
import common.model.UiState.Type
import follower.model.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.awt.Point
import java.awt.Rectangle

object UiStateHolder {
    private val mutex = Mutex()

    private val _state = MutableStateFlow(UiState.default)
    internal val state = _state.asStateFlow()

    suspend fun init(uiState: UiState) {
        _state.emit(uiState)
    }

    suspend fun update(
        isRunning: Boolean,
        connectionState: ConnectionState
    ) = mutex.withLock {
        _state.update {
            it.copy(
                isRunning = isRunning,
                connectionState = connectionState
            )
        }
    }

    suspend fun update(
        type: Type,
        state: UiState.CommonState
    ) = mutex.withLock {
        _state.update {
            when(type) {
                Type.X -> {
                    val xState = if(state.texts.firstOrNull()?.toIntOrNull() != null) {
                        state
                    } else {
                        this.state.value.xState
                    }
                    it.copy(xState = xState)
                }
                Type.Y -> {
                    val yState = if(state.texts.firstOrNull()?.toIntOrNull() != null) {
                        state
                    } else {
                        this.state.value.yState
                    }
                    it.copy(yState = yState)
                }
                Type.BUFF -> it.copy(buffState = state)
                Type.MAGIC_RESULT -> it.copy(magicResultState = state)
            }
        }
    }

    fun getCoordinates(): Point? {
        val state = state.value
        val x = state.xState.texts
            .firstOrNull()
            ?.replace(" ", "")
            ?.drop(1)
            ?.toIntOrNull()
        val y = state.yState.texts
            .firstOrNull()
            ?.replace(" ", "")
            ?.drop(1)
            ?.toIntOrNull()
        return if(x == null || y == null) null
        else Point(x, y)
    }

    fun getRectangle(type: Type): Rectangle {
        val state = state.value

        return when(type) {
            Type.X -> state.xState.rectangle
            Type.Y -> state.yState.rectangle
            Type.BUFF -> state.buffState.rectangle
            Type.MAGIC_RESULT -> state.magicResultState.rectangle
        }
    }
}