package common.model

import java.awt.Rectangle
import common.model.UiState.Type

sealed interface UiEvent {
    data object OnTryConnect: UiEvent
    data class OnRectangleChanged(
        val rectangle: Rectangle,
        val type: Type
    ): UiEvent
}