package follower.macro

import common.UiStateHolder
import common.model.UiState.Type
import common.network.OcrClient
import common.robot.DisplayProvider
import common.robot.Keyboard
import common.util.Result
import kotlinx.coroutines.*
import java.awt.Point
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

class MoveDetailAction(
    private val scope: CoroutineScope,
    private val ocrClient: OcrClient,
) {
    private enum class Direction { UP, DOWN, LEFT, RIGHT }
    private var commanderPoint: Point? = null

    var direction = false
    suspend fun moveTowards(myPoint: Point) = withContext(Dispatchers.IO) {
        val point = commanderPoint ?: return@withContext
        var deltaX = point.x - myPoint.x
        var deltaY = point.y - myPoint.y

//        println("deltaX: $deltaX, deltaY: $deltaY, ${FollowerMacro.property} $this")

        while (isActive) {
            if(FollowerMacro.property) return@withContext
            if(FollowerMacro.ctrlToggle.value) return@withContext

            releaseAll()
            if (abs(deltaX) <= 1 && abs(deltaY) <= 1) {
                return@withContext
            }

            direction = !direction
            if(direction) {
                if (deltaX > 1) {
                    if (tryMove(Direction.RIGHT)) deltaX -= 1
                } else if (deltaX < -1) {
                    if (tryMove(Direction.LEFT)) deltaX += 1
                }
                if (deltaY > 1) {
                    if (tryMove(Direction.DOWN)) deltaY -= 1
                } else if (deltaY < -1) {
                    if (tryMove(Direction.UP)) deltaY += 1
                }
            } else {
                if (deltaY > 1) {
                    if (tryMove(Direction.DOWN)) deltaY -= 1
                } else if (deltaY < -1) {
                    if (tryMove(Direction.UP)) deltaY += 1
                }
                if (deltaX > 1) {
                    if (tryMove(Direction.RIGHT)) deltaX -= 1
                } else if (deltaX < -1) {
                    if (tryMove(Direction.LEFT)) deltaX += 1
                }
            }

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
        val ctrlToggle = FollowerMacro.ctrlToggle.value
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
        delay(20)

        return true
    }
}