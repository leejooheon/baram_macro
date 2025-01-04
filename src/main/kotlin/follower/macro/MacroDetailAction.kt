package follower.macro

import common.robot.DisplayProvider
import common.model.UiState
import common.robot.Keyboard
import follower.model.MagicResultState
import follower.ocr.TextDetecter
import kotlinx.coroutines.*
import java.awt.event.KeyEvent

class MacroDetailAction {
    suspend fun honmasul() = withContext(Dispatchers.Default) {
        escape()
        while (isActive) {
            Keyboard.pressAndRelease(KeyEvent.VK_5)
            Keyboard.pressAndRelease(KeyEvent.VK_UP)
            Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
        }
    }

    suspend fun gongju() {
        tabTab()
        Keyboard.pressAndRelease(KeyEvent.VK_8)
        eat()
        gongJeung()
    }

    suspend fun tryGongJeung() {
        Keyboard.pressAndRelease(KeyEvent.VK_2)
//        healMe()
//        tabTab()
    }

    suspend fun gongJeung() = withContext(Dispatchers.IO) {
        var text: String
        var counter = 0
        val maxTryCount = 1
        while (isActive) {
            Keyboard.pressKeyRepeatedly(KeyEvent.VK_2, 2)

            val screen = DisplayProvider.capture(UiState.Type.MAGIC_RESULT)
            text = TextDetecter.detectString(screen)

            when {
                text.contains(MagicResultState.GONGJEUNG.tag) -> {
                    healMe()
                    tabTab()
                    break
                }

                text.contains(MagicResultState.NO_MP.tag) -> {
                    if(counter++ > maxTryCount) {
                        tabTab()
                        eat()
                        counter = 0
                    }
                }
                text.contains(MagicResultState.ME_DEAD.tag) -> {
                    FollowerMacro.obtainProperty()
                    dead(MagicResultState.ME_DEAD)
                    break
                }
            }
        }
    }

    suspend fun bomu() {
        focusMe(
            keyEvent = KeyEvent.VK_6,
            action = {
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
                Keyboard.pressAndRelease(KeyEvent.VK_7)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
            }
        )

        tabTab()
        Keyboard.pressAndRelease(KeyEvent.VK_6)
        Keyboard.pressAndRelease(KeyEvent.VK_7)
    }

    suspend fun dead(state: MagicResultState) {
        when(state) {
            MagicResultState.ME_DEAD -> {
                focusMe(
                    keyEvent = KeyEvent.VK_0,
                    action = {
                        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
                        Keyboard.pressAndRelease(KeyEvent.VK_1)
                        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
                    }
                )

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
        focusMe(
            keyEvent = KeyEvent.VK_1,
            action = {
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
                Keyboard.pressAndRelease(KeyEvent.VK_1)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
            }
        )
    }

    private suspend fun eat() {
        Keyboard.pressAndRelease(KeyEvent.VK_U,)
        delay(50)
        Keyboard.pressAndRelease(KeyEvent.VK_U)
    }

    suspend fun tabTab() {
        escape()
        Keyboard.pressAndRelease(KeyEvent.VK_TAB)
        delay(50)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB)
        delay(50)
    }

    private suspend inline fun focusMe(
        keyEvent: Int,
        crossinline action: suspend () -> Unit,
    ) {
        escape()
        Keyboard.pressAndRelease(keyEvent)
        delay(20)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME)
        delay(20)
        action.invoke()
    }

    suspend fun escape() {
        FollowerMacro.obtainProperty()
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        delay(20)
    }

    suspend fun invincible() {
        Keyboard.pressAndRelease(KeyEvent.VK_4)
    }
//    suspend fun invincible() = withContext(Dispatchers.IO) {
//        while (isActive) {
//            Keyboard.pressAndRelease(KeyEvent.VK_4)
//            delay(100)
//
//            val image = DisplayProvider.capture(UiState.Type.MAGIC_RESULT)
//            val result = TextDetecter.detectString(image)
//
//            when {
//                result.contains(MagicResultState.ALREADY.tag) -> break
//                result.contains(MagicResultState.NO_MP.tag) -> gongJeung()
//                result.contains(MagicResultState.ME_DEAD.tag) -> {
//                    dead(MagicResultState.ME_DEAD)
//                    break
//                }
//            }
//        }
//    }
}