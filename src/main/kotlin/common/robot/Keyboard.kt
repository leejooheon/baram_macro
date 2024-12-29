package common.robot

import kotlinx.coroutines.delay
import java.awt.Robot

object Keyboard {
    val robot = Robot()

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
}