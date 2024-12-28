package common.model

import follower.model.ConnectionState
import java.awt.Rectangle
import java.awt.image.BufferedImage

data class UiState(
    val connectionState: ConnectionState,
    val isRunning: Boolean,
    val xState: CommonState,
    val yState: CommonState,
    val buffState: CommonState,
    val magicResultState: CommonState,
) {
    data class CommonState(
        val texts: List<String>,
        val image: BufferedImage,
        val rectangle: Rectangle,
        val type: Type
    ) {
        companion object {
            val default = CommonState(
                texts = listOf("-"),
                image = defaultImage,
                rectangle = defaultRectangle,
                type = Type.X,
            )
        }
    }

    companion object {
        val defaultRectangle = Rectangle(0, 0, 100, 100)
        val defaultImage = BufferedImage(
            32,
            32,
            BufferedImage.TYPE_BYTE_BINARY
        )
        val default = UiState(
            connectionState = ConnectionState.Disconnected,
            isRunning = false,
            xState = CommonState.default.copy(type = Type.X),
            yState = CommonState.default.copy(type = Type.Y),
            buffState = CommonState.default.copy(type = Type.BUFF),
            magicResultState = CommonState.default.copy(type = Type.MAGIC_RESULT),
        )
    }
}