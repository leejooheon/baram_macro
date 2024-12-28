package follower.macro

import common.robot.Keyboard
import kotlinx.coroutines.delay
import java.awt.Point
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

class MoveDetailAction {
    private enum class Direction { UP, DOWN, LEFT, RIGHT }

    private val commanderPoint = AtomicReference<Point?>(null)

    fun update(point: Point) {
        commanderPoint.set(point)
    }

    suspend fun moveTowards(myPoint: Point?) {
        myPoint ?: return
        val point = commanderPoint.get() ?: return
        val deltaX = point.x - myPoint.x
        val deltaY = point.y - myPoint.y

        if(abs(deltaX) <= 1 && abs(deltaY) <= 1) {
            return
        }

        if (deltaX > 1) {
            tryMove(Direction.RIGHT)
        } else if (deltaX < -1) {
            tryMove(Direction.LEFT)
        }

        if (deltaY > 1) {
            tryMove(Direction.UP)
        } else if (deltaY < -1) {
            tryMove(Direction.DOWN)
        }
    }

    private suspend fun tryMove(
        direction: Direction,
    ) {
        val keyEvent = when(direction) {
            Direction.UP -> {
                println("KeyEvent.VK_UP")
                KeyEvent.VK_UP
            }
            Direction.DOWN -> {
                println("KeyEvent.VK_DOWN")
                KeyEvent.VK_DOWN
            }
            Direction.LEFT -> {
                println("KeyEvent.VK_LEFT")
                KeyEvent.VK_LEFT
            }
            Direction.RIGHT -> {
                println("KeyEvent.VK_RIGHT")
                KeyEvent.VK_RIGHT
            }
        }

        Keyboard.pressAndRelease(keyEvent, 100)
        delay((0.6).seconds)
    }

}