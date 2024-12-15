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
            magicResultRect = Rectangle(100,100,100,100),
            isRunning = false,
        )
    }
}