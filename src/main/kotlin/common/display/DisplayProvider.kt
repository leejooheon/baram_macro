package common.display

import common.UiStateHolder
import common.model.UiState
import common.robot.Keyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage

object DisplayProvider {
    suspend fun capture(
        type: UiState.Type
    ): BufferedImage = withContext(Dispatchers.IO) {
        val rectangle = UiStateHolder.getRectangle(type)
        val image = Keyboard.robot.createScreenCapture(rectangle)
        return@withContext image
    }
}