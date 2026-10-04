package jusulsa.model

data class JusulsaUiState(
    /** 남은 헬파이어 횟수 */
    val count: Int,
    /** 매크로가 돌고 있는지 */
    val isRunning: Boolean = false,
) {
    companion object {
        val default = JusulsaUiState(count = 0)
    }
}
