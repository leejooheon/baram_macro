package jusulsa

import common.robot.Keyboard
import follower.macro.FollowerMacro
import kotlinx.coroutines.*
import java.awt.Point
import java.awt.event.KeyEvent
import kotlin.math.abs

class MoveDetailHelper {
    private enum class Direction { UP, DOWN, LEFT, RIGHT }
    private var destinationPoint: Point? = null
    private var myPoint: Point? = null

    suspend fun moveTowards() = withContext(Dispatchers.IO) {
        val point = destinationPoint ?: return@withContext
        val myPoint = myPoint ?: return@withContext
        val deltaX = point.x - myPoint.x
        val deltaY = point.y - myPoint.y
        println("delta: $deltaX, $deltaY")

        if (abs(deltaX) <= 1 && abs(deltaY) <= 1) {
            return@withContext
        }

        if (deltaX > 0) {
            move(Direction.RIGHT)
        } else if (deltaX < 0) {
            move(Direction.LEFT)
        }
        if (deltaY > 1) {
            move(Direction.DOWN)
        } else if (deltaY < -1) {
            move(Direction.UP)
        }
    }

    internal fun updateCommander(point: Point) {
        destinationPoint = point
    }
    internal fun updateMe(point: Point) {
        myPoint = point
    }

    private suspend fun move(
        direction: Direction,
    ): Boolean {
        val keyEvent = when(direction) {
            Direction.UP -> KeyEvent.VK_UP
            Direction.DOWN -> KeyEvent.VK_DOWN
            Direction.LEFT -> KeyEvent.VK_LEFT
            Direction.RIGHT -> KeyEvent.VK_RIGHT
        }
        try {
            Keyboard.press(keyEvent)
            delay(300)
            Keyboard.release(keyEvent)
            delay(20)
        } catch (e: CancellationException) {
            Keyboard.release(keyEvent)
        }
        return true
    }
}