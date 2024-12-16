package follower.model

import java.awt.Rectangle

data class FollowerUiState(
    val connectionState: ConnectionState,
    val commandBuffer: List<String>,
    val magicRect: Rectangle,
    val buffStateRect: Rectangle,
    val isRunning: Boolean = false,
    ) {
    companion object {
        val default = FollowerUiState(
            connectionState = ConnectionState.Disconnected,
            commandBuffer = emptyList(),
            // 2050, 1100, 200, 30
            magicRect = Rectangle(2050,1100,200,30),
            buffStateRect = Rectangle(2090,855,230,100),
            isRunning = false,
        )
    }
}