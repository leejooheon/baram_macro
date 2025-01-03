package follower.model

import java.awt.Point

data class MacroUiState(
    val ctrlToggle: Boolean,
    val cycleTime: Long,
    val point: Point
) {
    companion object {
        val default = MacroUiState(
            ctrlToggle = false,
            cycleTime = 0,
            point = Point(0, 0)
        )
    }
}