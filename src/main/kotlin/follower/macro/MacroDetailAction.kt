package follower.macro

import common.base.UiStateHolder
import common.robot.Keyboard
import follower.model.MagicResultState
import follower.ocr.TextDetecter
import kotlinx.coroutines.*
import java.awt.event.KeyEvent
import kotlin.time.Duration.Companion.seconds

class MacroDetailAction(
    private val scope: CoroutineScope
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
        eat()
        gongJeung()
    }
    suspend fun tryGongJeung() {
        Keyboard.pressAndRelease(KeyEvent.VK_2)
//        healMe()
//        tabTab()
    }

    suspend fun gongJeung() = withContext(Dispatchers.IO) {
        val maxTryCount = 4
        var counter = 0
        while (isActive) {
            Keyboard.pressAndRelease(KeyEvent.VK_2)
            val state = UiStateHolder.state.value
            val text = state.magicResultState.texts.joinToString("\n")

            counter += 1
            when {
                text.contains("공력") -> {
                    healMe()
                    tabTab()
                    break
                }
                text.contains(MagicResultState.NO_MP.tag) -> {
                    if(counter >= maxTryCount) {
                        eat()
                        counter = 0
                    }
                }
                text.contains(MagicResultState.ME_DEAD.tag) -> {
                    dead(MagicResultState.ME_DEAD)
                    break
                }
                text.contains("공력") -> {
                    healMe()
                    tabTab()
                    break
                }
            }

            delay((0.25).seconds)
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
        Keyboard.pressAndRelease(KeyEvent.VK_4)

//        while (isActive) {
//            Keyboard.pressAndRelease(KeyEvent.VK_4)
//            val state = UiStateHolder.state.value
//            val magicState = state.magicResultState.texts.joinToString("\n")
//            println("magic: $magicState")
//            when {
//                magicState.contains("이미") -> break
//                magicState.contains("공력") -> break
//                magicState.contains(MagicResultState.NO_MP.tag) -> gongJeung()
//                magicState.contains(MagicResultState.ME_DEAD.tag) -> {
//                    dead(MagicResultState.ME_DEAD)
//                    break
//                }
//            }
//            delay((0.25).seconds)
//        }
    }
}