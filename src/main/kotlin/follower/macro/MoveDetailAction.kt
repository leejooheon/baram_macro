package follower.macro

import common.robot.Keyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.awt.Point
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

class MoveDetailAction {
    private enum class Direction { UP, DOWN, LEFT, RIGHT }

    private val commanderPoint = AtomicReference<Point?>(null)

    internal suspend fun moveTowards(myPoint: Point) = withContext(Dispatchers.IO) {
        val point = commanderPoint.get() ?: return@withContext
        var deltaX = point.x - myPoint.x
        var deltaY = point.y - myPoint.y

        while (isActive) {
            println("deltaX: $deltaX, deltaY: $deltaY")
            if (abs(deltaX) <= 1 && abs(deltaY) <= 1) {
                break
            }

            if (deltaX > 1) {
                if(tryMove(Direction.RIGHT)) deltaX -= 1
                else break
            } else if (deltaX < -1) {
                if(tryMove(Direction.LEFT)) deltaX += 1
                else break
            }

            if (deltaY > 1) {
                if(tryMove(Direction.DOWN)) deltaY -= 1
                else break
            } else if (deltaY < -1) {
                if(tryMove(Direction.UP)) deltaY += 1
                else break
            }
        }
    }

    internal fun update(point: Point) {
        commanderPoint.set(point)
    }

    private suspend fun tryMove(
        direction: Direction,
    ): Boolean {
        val property = FollowerMacro.property.get()
        val ctrlToggle = FollowerMacro.ctrlToggle.get()
        if(property || ctrlToggle) return false

        val keyEvent = when(direction) {
            Direction.UP -> KeyEvent.VK_UP
            Direction.DOWN -> KeyEvent.VK_DOWN
            Direction.LEFT -> KeyEvent.VK_LEFT
            Direction.RIGHT -> KeyEvent.VK_RIGHT
        }

        Keyboard.pressAndRelease(keyEvent, 300)

        return true
    }
}