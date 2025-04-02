package jusulsa.model

import java.awt.Rectangle
import java.awt.image.BufferedImage

data class JusulsaUiState(
    val count: Int,
    val addOnState: State,
    val resultState: State,
) {
    enum class JusulsaType {
        AddOn, Result
    }
    data class State(
        val texts: List<String>,
        val image: BufferedImage,
        val rectangle: Rectangle,
        val type: JusulsaType
    ) {
        companion object {
            val default = State(
                texts = listOf("-"),
                type = JusulsaType.AddOn,
                rectangle = Rectangle(10, 35, 200, 40),
                image = BufferedImage(
                    32,
                    32,
                    BufferedImage.TYPE_BYTE_BINARY
                ),
            )
        }
    }

    // x 1918, 2382
    // y 1158, 1088
    companion object {
        val default = JusulsaUiState(
            count = 0,
            addOnState = State.default.copy(
                type = JusulsaType.AddOn,
                rectangle = Rectangle(1267, 60, 170, 60)
            ),
            resultState = State.default.copy(
                type = JusulsaType.Result,
                rectangle = Rectangle(1280, 750, 170, 30)
            )
        )
    }
}