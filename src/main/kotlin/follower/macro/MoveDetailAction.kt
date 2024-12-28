package follower.macro

import common.robot.Keyboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Point
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

class MoveDetailAction(
    private val scope: CoroutineScope
) {
    private enum class Direction { UP, DOWN, LEFT, RIGHT }

    private val commanderPoint = AtomicReference<Point?>(null)
    private var moveJob: Job? = null

    fun update(point: Point) {
        commanderPoint.set(point)
    }

    fun moveTowards(myPoint: Point?) {
        moveJob?.cancel()
        moveJob = scope.launch {
            myPoint ?: return@launch
            val point = commanderPoint.get() ?: return@launch
            val deltaX = point.x - myPoint.x
            val deltaY = point.y - myPoint.y
            println("deltaX: $deltaX, deltaY: $deltaY")
            if (abs(deltaX) <= 1 && abs(deltaY) <= 1) {
                return@launch
            }

            if (deltaX > 1) {
                tryMove(Direction.RIGHT)
            } else if (deltaX < -1) {
                tryMove(Direction.LEFT)
            }

            if (deltaY > 1) {
                tryMove(Direction.DOWN)
            } else if (deltaY < -1) {
                tryMove(Direction.UP)
            }
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
        delay((0.25).seconds)
    }
}