package follower.model

import java.awt.Rectangle

data class FollowerUiState(
    val connectionState: ConnectionState,
    val commandBuffer: List<String>,
    val magicResultRect: Rectangle,
    val isRunning: Boolean = false,
    ) {
    companion object {
        val default = FollowerUiState(
            connectionState = ConnectionState.Disconnected,
            commandBuffer = emptyList(),
            // 2050, 1100, 200, 30
            magicResultRect = Rectangle(2050,1100,200,30),
            isRunning = false,
        )
    }
}