package common.display

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage

class DisplayProvider(private val robot: Robot) {
    suspend fun capture(
        rectangle: Rectangle
    ): BufferedImage = withContext(Dispatchers.IO) {
        val image = robot.createScreenCapture(rectangle)
        return@withContext image
    }
}