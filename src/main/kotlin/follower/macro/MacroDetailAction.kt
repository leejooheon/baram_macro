package follower.macro

import common.robot.DisplayProvider
import common.model.UiState
import common.robot.Keyboard
import follower.model.MagicResultState
import follower.ocr.TextDetecter
import kotlinx.coroutines.*
import java.awt.event.KeyEvent
import kotlin.random.Random
import kotlin.time.Duration

class MacroDetailAction {
    suspend fun loop(keyEvent: Int) = withContext(Dispatchers.IO) {
//        escape()
        try {
            while (isActive) {
                Keyboard.pressAndRelease(keyEvent)
                Keyboard.pressAndRelease(KeyEvent.VK_UP)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
                delay(20)
            }
        } catch (e: Exception) {
            delay(60)
            Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
            delay(60)
            Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        }
    }
    suspend fun chumchum() {

        Keyboard.pressAndRelease(KeyEvent.VK_TAB)
        delay(60)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB)
        delay(60)

    }
    suspend fun mabeAroundMe() {
        listOf(
            KeyEvent.VK_UP,
            KeyEvent.VK_LEFT,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_RIGHT
        ).forEach {
            val duration = 20L
            Keyboard.pressAndRelease(KeyEvent.VK_8)
            delay(duration)
            Keyboard.pressAndRelease(KeyEvent.VK_HOME)
            delay(duration)
            Keyboard.pressAndRelease(it)
            delay(duration)
            Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
            delay(duration)
        }
    }
    suspend fun honmasul() = withContext(Dispatchers.IO) {
        val duration = 33L
//        escape()
        try {
            while (isActive) {
                Keyboard.pressAndRelease(KeyEvent.VK_8)
                Keyboard.pressAndRelease(KeyEvent.VK_UP)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
                delay(120)
//                Keyboard.pressAndRelease(KeyEvent.VK_7)
//                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
//                delay(125)
            }
        } catch (e: Exception) {
            Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        }
    }
    suspend fun julmang() = withContext(Dispatchers.IO) {
        val directions = listOf(
            KeyEvent.VK_UP,
            KeyEvent.VK_LEFT,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_RIGHT
        )
        try {
            var cnt = 0
            var directionIndex = 0
            while (isActive) {
                if(cnt %16 == 3) {
                    directionIndex = (directionIndex + 1) % directions.size
                }
                if (cnt % 16 > 8) {
                    Keyboard.pressAndRelease(KeyEvent.VK_3)
                    Keyboard.pressAndRelease(KeyEvent.VK_HOME)
                    Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
                    delay(300)
                } else {
                    Keyboard.pressAndRelease(KeyEvent.VK_7)
                    Keyboard.pressAndRelease(directions[directionIndex])
                    Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
                    delay(120)
                }
                cnt++
            }
        } catch (e: Exception) {
            Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        }
    }

    suspend fun honmasul(duration: Duration) = withContext(Dispatchers.IO) {
        withTimeout(duration) {
            escape()
            while (isActive) {
                Keyboard.pressAndRelease(KeyEvent.VK_5)
                Keyboard.pressAndRelease(KeyEvent.VK_UP)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
            }
        }
        escape()
        tabTab()
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

    suspend fun test() {
//        Keyboard.pressAndRelease(KeyEvent.VK_2)
//        delay(20)
//        Keyboard.pressAndRelease(KeyEvent.VK_SPACE)
//        delay(20)
//        Keyboard.pressAndRelease(KeyEvent.VK_3)
//        delay(20)
//        Keyboard.pressAndRelease(KeyEvent.VK_1)

        Keyboard.pressAndRelease(KeyEvent.VK_2)
        delay(20)
        Keyboard.pressAndRelease(KeyEvent.VK_3)
        delay(20)
        Keyboard.pressAndRelease(KeyEvent.VK_SPACE)
        delay(20)
        Keyboard.pressAndRelease(KeyEvent.VK_3)
        delay(450)
        Keyboard.pressAndRelease(KeyEvent.VK_SPACE)
    }
    suspend fun test2() {

        Keyboard.pressAndRelease(KeyEvent.VK_TAB)
    }

    suspend fun healMe() {
        escape()
        focusMe(
            keyEvent = KeyEvent.VK_1,
            action = {
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
            }
        )
    }

    suspend fun eat(delay: Long = 100) {
        FollowerMacro.obtainProperty()

        delay(60)
        Keyboard.pressAndRelease(KeyEvent.VK_U, delay)
        delay(60)
        Keyboard.pressAndRelease(KeyEvent.VK_U, delay)
    }

    suspend fun tabTab() {
//        escape()
        Keyboard.pressAndRelease(KeyEvent.VK_TAB)
        delay(30)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME)
        delay(30)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB)
        delay(30)
    }
    suspend fun bomuMe() {
        Keyboard.pressAndRelease(KeyEvent.VK_9)
        delay(60)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME)
        delay(60)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
        delay(60)
        Keyboard.press(KeyEvent.VK_SHIFT)
        delay(60)
        Keyboard.pressAndRelease(KeyEvent.VK_Z)
        Keyboard.release(KeyEvent.VK_SHIFT)
        delay(60)
        Keyboard.pressAndRelease(KeyEvent.VK_V)
        delay(60)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
    }
    suspend fun heal(time: Int) {
        Keyboard.pressKeyRepeatedly(
            keyEvent = KeyEvent.VK_1,
            time = time,
            delay = Random.nextLong(20, 60)
        )
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
//        FollowerMacro.obtainProperty()
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        delay(66)
    }

//    suspend fun invincible() {
//        Keyboard.pressAndRelease(KeyEvent.VK_4)
//    }
    suspend fun invincible() = withContext(Dispatchers.IO) {
        while (isActive) {
            Keyboard.pressAndRelease(KeyEvent.VK_4)
            delay(250)

            val image = DisplayProvider.capture(UiState.Type.MAGIC_RESULT)
            val result = TextDetecter.detectString(image)

            when {
                result.contains(MagicResultState.ALREADY.tag) -> break
            }
        }
    }

    suspend fun gongJeung() = withContext(Dispatchers.IO) {
        var text: String
        var counter = 0
        val maxTryCount = 1
        while (isActive) {
            Keyboard.pressAndRelease(KeyEvent.VK_2)
            delay(250)

            val screen = DisplayProvider.capture(UiState.Type.MAGIC_RESULT)
            text = TextDetecter.detectStringRemote(screen)
            println("gongJeung: $text")
            when {
                text.contains(MagicResultState.GONGJEUNG.tag) -> {
                    healMe()
                    tabTab()
                    break
                }

                text.contains(MagicResultState.NO_MP.tag) -> {
                    if(counter++ > maxTryCount) {
                        FollowerMacro.obtainProperty()
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
}