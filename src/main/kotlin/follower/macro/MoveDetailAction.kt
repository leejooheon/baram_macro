package follower.macro

import common.network.OcrClient
import common.robot.Keyboard
import kotlinx.coroutines.*
import java.awt.Point
import java.awt.event.KeyEvent
import kotlin.math.abs

class MoveDetailAction(
    private val scope: CoroutineScope,
    private val ocrClient: OcrClient,
) {
    private enum class Direction { UP, DOWN, LEFT, RIGHT }
    private var commanderPoint: Point? = null
    private var myPoint: Point? = null

    suspend fun moveTowards() = withContext(Dispatchers.IO) {
        while (isActive) {
            if(FollowerMacro.property) return@withContext
            if(FollowerMacro.ctrlToggle.value) return@withContext

            val point = commanderPoint ?: return@withContext
            val myPoint = myPoint ?: return@withContext
            var deltaX = point.x - myPoint.x
            var deltaY = point.y - myPoint.y

            if (abs(deltaX) <= 0 && abs(deltaY) <= 1 || abs(deltaX) <= 1 && abs(deltaY) <= 0) {
                return@withContext
            }

            if (deltaX > 0) tryMove(Direction.RIGHT)
            else if (deltaX < 0) tryMove(Direction.LEFT)

            if (deltaY > 0) tryMove(Direction.DOWN)
            else if (deltaY < 0) tryMove(Direction.UP)
        }
    }

    internal fun updateCommander(point: Point) {
        commanderPoint = point
    }
    internal fun updateMe(point: Point) {
        myPoint = point
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
    ) {
        val property = FollowerMacro.property
        val ctrlToggle = FollowerMacro.ctrlToggle.value
        if(property || ctrlToggle) return

        val keyEvent = when(direction) {
            Direction.UP -> KeyEvent.VK_UP
            Direction.DOWN -> KeyEvent.VK_DOWN
            Direction.LEFT -> KeyEvent.VK_LEFT
            Direction.RIGHT -> KeyEvent.VK_RIGHT
        }

        Keyboard.press(keyEvent)
        delay(300)
        Keyboard.release(keyEvent)
        delay(20)
    }
}