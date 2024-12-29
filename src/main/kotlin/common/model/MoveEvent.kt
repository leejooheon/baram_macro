package common.model

import java.awt.Point

sealed interface MoveEvent {
    data class OnCommanderPositionChanged(val point: Point): MoveEvent
    data object OnMove: MoveEvent
}