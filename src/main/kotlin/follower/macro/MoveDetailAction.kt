package follower.macro

import common.robot.Keyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

        releaseAll()
        while (isActive) {
//            println("deltaX: $deltaX, deltaY: $deltaY, ${FollowerMacro.property} $this")
            if(FollowerMacro.property) break
            if (abs(deltaX) <= 1 && abs(deltaY) <= 1) {
                break
            }
            if (deltaX > 1) {
                if(tryMove(Direction.RIGHT)) deltaX -= 1
                else break
            } else if (deltaX < -1) {
                if(tryMove(Direction.LEFT)) deltaX += 1
                else break
            } else if (deltaY > 1) {
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

    internal fun releaseAll() {
        listOf(
            KeyEvent.VK_UP,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_LEFT,
            KeyEvent.VK_RIGHT,
        ).forEach {
            Keyboard.release(it)
        }
    }

    private suspend fun tryMove(
        direction: Direction,
    ): Boolean {
        val property = FollowerMacro.property
        val ctrlToggle = FollowerMacro.ctrlToggle
        if(property || ctrlToggle) return false

        val keyEvent = when(direction) {
            Direction.UP -> KeyEvent.VK_UP
            Direction.DOWN -> KeyEvent.VK_DOWN
            Direction.LEFT -> KeyEvent.VK_LEFT
            Direction.RIGHT -> KeyEvent.VK_RIGHT
        }

        Keyboard.press(keyEvent)
        delay(250)
        Keyboard.release(keyEvent)

        return true
    }
}