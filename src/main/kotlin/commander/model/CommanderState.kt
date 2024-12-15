package commander.model

data class CommanderState(
    val clientHostAddressList: List<String>
) {
    companion object {
        val default = CommanderState(emptyList())
    }
}