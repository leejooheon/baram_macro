package common

import kotlinx.coroutines.delay
import java.awt.Robot

object Keyboard {
    private val robot = Robot()

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
}