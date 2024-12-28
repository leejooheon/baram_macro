package follower.macro

import common.robot.Keyboard
import kotlinx.coroutines.delay
import java.awt.Point
import java.awt.event.KeyEvent
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

class MoveDetailAction {
    private enum class Direction { UP, DOWN, LEFT, RIGHT }

    suspend fun moveTowards(myPoint: Point, commanderPoint: Point) {
        val deltaX = commanderPoint.x - myPoint.x
        val deltaY = commanderPoint.y - myPoint.y

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
            Direction.UP -> KeyEvent.VK_UP
            Direction.DOWN -> KeyEvent.VK_DOWN
            Direction.LEFT -> KeyEvent.VK_LEFT
            Direction.RIGHT -> KeyEvent.VK_RIGHT
        }
        Keyboard.pressAndRelease(keyEvent, 100)
        delay((0.6).seconds)
    }

}