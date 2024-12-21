package follower.macro

import common.Keyboard
import follower.model.MagicResultState
import follower.ocr.TextDetecter
import kotlinx.coroutines.*
import java.awt.event.KeyEvent

class MacroDetailAction(
    private val textDetector: TextDetecter
) {
    private val failureTargets = listOf("실패", "심패")

    suspend fun honmasul() = withContext(Dispatchers.Default) {
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        while (isActive) {
            Keyboard.pressAndRelease(KeyEvent.VK_5)
            Keyboard.pressAndRelease(KeyEvent.VK_UP)
            Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
        }
    }

    suspend fun gongju() {
        escape()
        tabTab()

        Keyboard.pressAndRelease(KeyEvent.VK_8)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)

        eat()
        gongJeung()
    }

    suspend fun gongJeung() = withContext(Dispatchers.IO) { // F2
        var text: String
        while (isActive) {
            Keyboard.pressAndRelease(KeyEvent.VK_2)
            text = textDetector.detectString(FollowerMacro2.magicRect)
            println("@@@ gongjeung: $text")

            when {
                failureTargets.contains(text) -> continue
                text.contains(MagicResultState.NO_MP.tag) -> eat()
                text.contains(MagicResultState.ME_DEAD.tag) -> {
                    dead(true)
                    break
                }
                text.contains("공력") -> break
            }
        }
    }

    suspend fun bomu() {
        focusMe(KeyEvent.VK_6)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
        Keyboard.pressAndRelease(KeyEvent.VK_7)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)

        tabTab()
        Keyboard.pressAndRelease(KeyEvent.VK_6)
        Keyboard.pressAndRelease(KeyEvent.VK_7)
    }

    suspend fun dead(flag: Boolean) {
        if(flag) {
            focusMe(KeyEvent.VK_0)
            Keyboard.pressAndRelease(KeyEvent.VK_ENTER)

            Keyboard.pressAndRelease(KeyEvent.VK_1)
            Keyboard.pressAndRelease(KeyEvent.VK_ENTER)

            gongJeung()
        } else {
            tabTab()
            Keyboard.pressAndRelease(KeyEvent.VK_0)
        }
    }

    private suspend fun eat() {
        Keyboard.pressAndRelease(KeyEvent.VK_U)
        Keyboard.pressAndRelease(KeyEvent.VK_U)
    }

    private suspend fun tabTab() {
        escape()
        Keyboard.pressAndRelease(KeyEvent.VK_TAB)
        delay(20)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB)
    }

    private suspend fun focusMe(keyEvent: Int) {
        escape()
        Keyboard.pressAndRelease(keyEvent)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME)
        delay(20)
    }

    private suspend fun escape() {
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
//        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
//        delay(20)
    }

    suspend fun invincible() = withContext(Dispatchers.IO) {
        do {
            Keyboard.pressAndRelease(KeyEvent.VK_4)
        } while (textDetector.detect(failureTargets, FollowerMacro2.magicRect))
    }
}