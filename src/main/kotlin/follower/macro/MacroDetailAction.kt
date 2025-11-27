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
import kotlin.time.Duration.Companion.seconds

class MacroDetailAction {
    companion object {
        const val HELLFIRE = KeyEvent.VK_1
        const val GONGJEUNG = KeyEvent.VK_2
        const val MABEE = KeyEvent.VK_3
        const val HWALRYUCK = KeyEvent.VK_4
        const val CHUM1 = KeyEvent.VK_5
        const val JULMANG = KeyEvent.VK_6
        const val JUNGDOK = KeyEvent.VK_7
        const val JEOJU = KeyEvent.VK_8
        const val HEAL = KeyEvent.VK_9
        const val JIPOK = KeyEvent.VK_0
    }
    suspend fun test() = withContext(Dispatchers.IO){
        while (isActive) {
            val key = listOf(
                KeyEvent.VK_UP,
                KeyEvent.VK_LEFT,
                KeyEvent.VK_DOWN,
                KeyEvent.VK_RIGHT
            ).random()
            Keyboard.pressAndRelease(key)
            delay(15.seconds)
        }
    }

    suspend fun mabeAroundMe() {

        listOf(
            KeyEvent.VK_UP,
            KeyEvent.VK_LEFT,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_RIGHT
        ).forEach {
            val duration = 30L
            Keyboard.pressAndRelease(MABEE)
            delay(duration)
            Keyboard.pressAndRelease(KeyEvent.VK_HOME)
            delay(duration)
            Keyboard.pressAndRelease(it)
            delay(duration)
            Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
            delay(duration)
        }
    }

    suspend fun mabee() = withContext(Dispatchers.IO) {
        try {
            while (isActive) {
                Keyboard.pressAndRelease(MABEE, 10)
                Keyboard.pressAndRelease(KeyEvent.VK_UP, 10)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER, 20)
                Keyboard.pressAndRelease(JEOJU, 10)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER, 10)
                delay(20)
            }
        } catch (e: Exception) {
            Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        }
    }
    suspend fun jeoju() = withContext(Dispatchers.IO) {
        try {
            while (isActive) {
                Keyboard.pressAndRelease(JEOJU)
                Keyboard.pressAndRelease(KeyEvent.VK_UP)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
                delay(80)
            }
        } catch (e: Exception) {
            Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        }
    }

    suspend fun julmang() = withContext(Dispatchers.IO) {
        try {
            while (isActive) {
                Keyboard.pressAndRelease(JULMANG)
                Keyboard.pressAndRelease(KeyEvent.VK_UP)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
                delay(80)
            }
        } catch (e: Exception) {
            Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        }
    }

    suspend fun jungdok() = withContext(Dispatchers.IO) {
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
                    Keyboard.pressAndRelease(HEAL)
                    Keyboard.pressAndRelease(KeyEvent.VK_HOME)
                    Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
                    delay(300)
                } else {
                    Keyboard.pressAndRelease(JUNGDOK)
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

    suspend fun jeoju(duration: Duration) = withContext(Dispatchers.IO) {
        withTimeout(duration) {
            escape()
            while (isActive) {
                Keyboard.pressAndRelease(JEOJU)
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
        val duration = 0L
        Keyboard.pressAndRelease(KeyEvent.VK_TAB)
        delay(duration)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME)
        delay(duration)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB)
        delay(duration)
    }
    suspend fun bomuMe() {
        val duration = 30L
        Keyboard.press(KeyEvent.VK_SHIFT)
        delay(duration)
        Keyboard.pressAndRelease(KeyEvent.VK_Z)
        Keyboard.release(KeyEvent.VK_SHIFT)
        delay(duration)
        Keyboard.pressAndRelease(KeyEvent.VK_K)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
        Keyboard.press(KeyEvent.VK_SHIFT)
        delay(duration)
        Keyboard.pressAndRelease(KeyEvent.VK_Z)
        Keyboard.release(KeyEvent.VK_SHIFT)
        delay(duration)
        Keyboard.pressAndRelease(KeyEvent.VK_V)
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