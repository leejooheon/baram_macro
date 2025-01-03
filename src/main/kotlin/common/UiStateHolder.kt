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
import java.awt.image.BufferedImage

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
    ): UiState = mutex.withLock {
        val uiState = this@UiStateHolder.state.value
        val value = when(type) {
            Type.X -> uiState.copy(xState = state)
            Type.Y -> uiState.copy(yState = state)
            Type.BUFF -> uiState.copy(buffState = state)
            Type.MAGIC_RESULT -> uiState.copy(magicResultState = state)
            else -> throw IllegalArgumentException("잘못된 타입: $type")
        }
        _state.emit(value)
        return@withLock value
    }

    suspend fun updateCoordinates(
        x: String,
        y: String,
        xScreen: BufferedImage,
        yScreen: BufferedImage,
        time: Long,
    ) {
        val model = _state.value
        val newState = model.copy(
            xState = model.xState.copy(
                texts = listOf(x),
                image = xScreen,
            ),
            yState = model.yState.copy(
                texts = listOf(y),
                image = yScreen,
            ),
            ocrDetectionTime = time
        )
        _state.emit(newState)
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