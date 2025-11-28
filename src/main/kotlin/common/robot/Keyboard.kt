package common.robot

import kotlinx.coroutines.*
import java.awt.Robot
import java.awt.event.InputEvent

object Keyboard {
    val robot = Robot()

    suspend fun pressKeyRepeatedly(keyEvent: Int, time: Int, delay: Long = 20) {
        repeat(time) {
            pressAndRelease(keyEvent, delay)
        }
    }

    suspend fun pressAndRelease(keyEvent: Int, delay: Long = 10) {
        try {
            press(keyEvent)
            delay(delay)
        } finally {
            withContext(NonCancellable) {
                release(keyEvent)
                delay(delay)
            }
        }
    }
    fun press(keyEvent: Int) {
        robot.keyPress(keyEvent)
    }
    fun release(keyEvent: Int) {
        robot.keyRelease(keyEvent)
    }

    suspend fun mouseClick(
        x: Float,
        y: Float,
        button: Int = InputEvent.BUTTON1_DOWN_MASK
    ) = withContext(Dispatchers.IO) {
        robot.mouseMove(x.toInt(), y.toInt())
        robot.mousePress(button)
        delay(20)
        robot.mouseRelease(button)
    }
    suspend fun mouseClick(
        button: Int = InputEvent.BUTTON1_DOWN_MASK
    ) = withContext(Dispatchers.IO) {
        robot.mousePress(button)
        delay(20)
        robot.mouseRelease(button)
    }
}