package common

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.delay
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.lang.Exception

object Keyboard {
    private val robot = Robot()

    suspend fun pressKeyRepeatedly(keyEvent: Int, time: Int, delay: Long = 20) {
        repeat(time) {
            pressAndRelease(keyEvent, delay)
        }
    }


    suspend fun pressAndRelease(keyEvent: Int, delay: Long = 20) {
        press(keyEvent)
        delay(delay)
        release(keyEvent)
        delay(delay)
    }
    fun press(keyEvent: Int) {
        robot.keyPress(keyEvent)
    }
    fun release(keyEvent: Int) {
        robot.keyRelease(keyEvent)
    }

    fun capture(rectangle: Rectangle): ImageBitmap {
        val image = robot.createScreenCapture(Rectangle(Toolkit.getDefaultToolkit().screenSize))

        return try {
            image.getSubimage(rectangle.x, rectangle.y, rectangle.width, rectangle.height)
        } catch (e: Exception) {
            image
        }.toComposeImageBitmap()
    }
}