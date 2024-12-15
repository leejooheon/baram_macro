package commander.model

data class CommanderUiState(
    val commanderState: CommanderState,
    val commandBuffer: List<String>,
    val isRunning: Boolean = false,
) {
    companion object {
        val default = CommanderUiState(
            commanderState = CommanderState(emptyList()),
            commandBuffer = emptyList(),
            isRunning = false,
        )
    }
}