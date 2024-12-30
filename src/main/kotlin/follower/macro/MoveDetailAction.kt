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
    private var commanderPoint: Point? = null

    internal suspend fun moveTowards(myPoint: Point) = withContext(Dispatchers.Default) {
        val point = commanderPoint ?: return@withContext
        var deltaX = point.x - myPoint.x
        var deltaY = point.y - myPoint.y

        var direction = true
        while (isActive) {
//            println("deltaX: $deltaX, deltaY: $deltaY, ${FollowerMacro.property} $this")
            if(FollowerMacro.property) break
            releaseAll()
            if (abs(deltaX) <= 1 && abs(deltaY) <= 1) {
                break
            }
            if(direction) {
                if (deltaX > 1) {
                    if (tryMove(Direction.RIGHT)) deltaX -= 2
                    else break
                } else if (deltaX < -1) {
                    if (tryMove(Direction.LEFT)) deltaX += 2
                    else break
                }
                if (deltaY > 1) {
                    if (tryMove(Direction.DOWN)) deltaY -= 2
                    else break
                } else if (deltaY < -1) {
                    if (tryMove(Direction.UP)) deltaY += 2
                    else break
                }
            } else {
                if (deltaY > 1) {
                    if (tryMove(Direction.DOWN)) deltaY -= 2
                    else break
                } else if (deltaY < -1) {
                    if (tryMove(Direction.UP)) deltaY += 2
                    else break
                }
                if (deltaX > 1) {
                    if (tryMove(Direction.RIGHT)) deltaX -= 2
                    else break
                } else if (deltaX < -1) {
                    if (tryMove(Direction.LEFT)) deltaX += 2
                    else break
                }
            }
            direction = !direction
        }
    }

    internal fun update(point: Point) {
        commanderPoint = point
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
        delay(350)
        Keyboard.release(keyEvent)

        return true
    }
}