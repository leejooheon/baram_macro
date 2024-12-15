package common

import kotlinx.coroutines.delay
import java.awt.Robot

object Keyboard {
    private val robot = Robot()

    suspend fun event(keyEvent: Int, delay: Long = 20) {
        robot.keyPress(keyEvent)
        delay(delay)
        robot.keyRelease(keyEvent)
    }
    fun press(keyEvent: Int) {
        robot.keyPress(keyEvent)
    }
    fun release(keyEvent: Int) {
        robot.keyRelease(keyEvent)
    }
}