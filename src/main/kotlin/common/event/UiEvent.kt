package common.event

import common.model.Type
import java.awt.Rectangle

sealed interface UiEvent {
    data object OnTryConnect: UiEvent
    data class OnRectangleChanged(
        val rectangle: Rectangle,
        val type: Type
    ): UiEvent
}