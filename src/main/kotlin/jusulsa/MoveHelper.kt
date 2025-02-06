package jusulsa

import common.UiStateHolder
import common.robot.Keyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MoveHelper {
    suspend fun goToDungeon() = withContext(Dispatchers.IO) {
        val duration = 0.5.seconds
        delay(duration)
        noranbiseo()
        delay(duration)
        biyoung(KeyEvent.VK_3)
        delay(duration)
        chulDu(DUNGEON_NICKNAME)
    }

    suspend fun goToKing() = withContext(Dispatchers.IO) {
        biyoung(KeyEvent.VK_3)
        delay(0.5.seconds)
        chulDu(KING_NICKNAME)
    }

    private suspend fun noranbiseo() {
        Keyboard.press(KeyEvent.VK_CONTROL)
        delay(60)
        Keyboard.pressAndRelease(KeyEvent.VK_L)
        delay(60)
        Keyboard.release(KeyEvent.VK_CONTROL)
    }

    private suspend fun biyoung(direction: Int) {

        val duration = 100.milliseconds
        Keyboard.press(KeyEvent.VK_SHIFT)
        delay(duration)
        Keyboard.pressAndRelease(KeyEvent.VK_Z)
        delay(duration)
        Keyboard.release(KeyEvent.VK_SHIFT)
        delay(duration)
        Keyboard.pressAndRelease(KeyEvent.VK_Z)
        delay(duration)
        Keyboard.pressAndRelease(direction)
        delay(duration)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
    }

    private suspend fun chulDu(target: String) = withContext(Dispatchers.IO) {
        val duration = 60L
        Keyboard.press(KeyEvent.VK_SHIFT)
        delay(duration)
        Keyboard.pressAndRelease(KeyEvent.VK_Z)
        delay(duration)
        Keyboard.release(KeyEvent.VK_SHIFT)
        delay(duration)
        Keyboard.pressAndRelease(KeyEvent.VK_X)
        delay(duration)
        sendText(target)
        delay(duration)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
    }

    private fun sendText(text: String) {
        val stringSelection = StringSelection(text)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(stringSelection, null)
        val robot = Keyboard.robot
        robot.keyPress(KeyEvent.VK_CONTROL)
        robot.keyPress(KeyEvent.VK_V)
        robot.keyRelease(KeyEvent.VK_V)
        robot.keyRelease(KeyEvent.VK_CONTROL)
    }

    companion object {
        private val KING_NICKNAME = "차렷"
        private val DUNGEON_NICKNAME = "붝소"
    }
}