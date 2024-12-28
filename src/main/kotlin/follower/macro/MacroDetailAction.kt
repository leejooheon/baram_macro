package follower.macro

import common.robot.Keyboard
import follower.model.MagicResultState
import follower.ocr.TextDetecter
import kotlinx.coroutines.*
import java.awt.event.KeyEvent

class MacroDetailAction {
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
        eat()
        gongJeung()
    }
    suspend fun tryGongJeung() {
        Keyboard.pressAndRelease(KeyEvent.VK_2)
        healMe()
        tabTab()
    }

    suspend fun gongJeung() = withContext(Dispatchers.IO) {
//        var text: String
//        val maxTryCount = 3
//        var counter = 0
//        while (isActive) {
//            Keyboard.pressAndRelease(KeyEvent.VK_2)
//            text = TextDetecter.detectString(FollowerMacro.magicRect)
//            counter += 1
//            when {
//                failureTargets.contains(text) -> continue
//                text.contains(MagicResultState.NO_MP.tag) -> {
//                    if(counter >= maxTryCount) {
//                        eat()
//                        counter = 0
//                    }
//                }
//                text.contains(MagicResultState.ME_DEAD.tag) -> {
//                    dead(MagicResultState.ME_DEAD)
//                    break
//                }
//                text.contains("공력") -> {
//                    healMe()
//                    tabTab()
//                    break
//                }
//            }
//        }
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

    suspend fun dead(state: MagicResultState) {
        when(state) {
            MagicResultState.ME_DEAD -> {
                focusMe(KeyEvent.VK_0)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)

                Keyboard.pressAndRelease(KeyEvent.VK_1)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)

                gongJeung()
                invincible()
            }
            MagicResultState.OTHER_DEAD -> {
                tabTab()
                Keyboard.pressAndRelease(KeyEvent.VK_0)
            }
            else -> throw IllegalArgumentException("invalidArgument!!: $state")
        }
    }

    private suspend fun healMe() {
        escape()
        focusMe(KeyEvent.VK_1)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
    }

    private suspend fun eat() {
        Keyboard.pressAndRelease(KeyEvent.VK_U)
        Keyboard.pressAndRelease(KeyEvent.VK_U)
    }

    suspend fun tabTab() {
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

    suspend fun escape() {
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
//        delay(20)
    }

    suspend fun invincible() = withContext(Dispatchers.IO) {
//        while (isActive) {
//            Keyboard.pressAndRelease(KeyEvent.VK_4)
//            val result = TextDetecter.detectString(FollowerMacro.magicRect)
//            println("invincible: $result")
//            when {
//                result.contains("이미") -> break
//                result.contains(MagicResultState.NO_MP.tag) -> gongJeung()
//                result.contains(MagicResultState.ME_DEAD.tag) -> {
//                    dead(MagicResultState.ME_DEAD)
//                    break
//                }
//            }
//        }
    }
}