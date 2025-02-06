package common.model

import kotlinx.serialization.Serializable

@Serializable
data class PositionResult(
    val text: String,
    val confidence: Float,
    val position: MyPosition
) {
    @Serializable
    data class MyPosition(
        val x: Float,
        val y: Float,
    )
}