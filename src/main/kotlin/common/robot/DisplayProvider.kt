package common.robot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Rectangle
import java.awt.image.BufferedImage

object DisplayProvider {
    suspend fun capture(
        rectangle: Rectangle
    ): BufferedImage = withContext(Dispatchers.IO) {
        Keyboard.robot.createScreenCapture(rectangle)
    }
}
