package follower.model

data class FollowerUiState(
    val connectionState: ConnectionState,
    val commandBuffer: List<String>,
    val isRunning: Boolean = false,

) {
    companion object {
        val default = FollowerUiState(
            connectionState = ConnectionState.Disconnected,
            commandBuffer = emptyList(),
            isRunning = false,
        )
    }
}