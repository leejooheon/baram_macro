package follower.macro

import common.robot.Keyboard
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.awt.event.KeyEvent
import kotlin.time.Duration.Companion.seconds

class MacroDetailAction2 {
    private var bomuTime = 0L
    private var latestDirection: Int = KeyEvent.VK_LEFT

    fun onDirectionChanged(event: Int) {
        latestDirection = event
    }

    suspend fun hellfire() {
        focusMe(
            keyEvent = JEOJU,
            action = {
                Keyboard.pressAndRelease(latestDirection)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
                Keyboard.pressAndRelease(HELLFIRE)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
            }
        )
    }

    suspend fun heal() {
        healMe()
        heal(3)

        if(System.currentTimeMillis() > bomuTime + 160.seconds.inWholeMilliseconds) {
            bomu(false)
            bomuTime = System.currentTimeMillis()
        }

        tabTab()
        while (true) {
            currentCoroutineContext().ensureActive()
            heal(3)
            delay(1.seconds)
        }
    }

    suspend fun mabeAroundMe() {
        val duration = 20L
        listOf(
            KeyEvent.VK_UP,
            KeyEvent.VK_LEFT,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_RIGHT
        ).forEach {
            focusMe(
                keyEvent = MABEE,
                action = {
                    Keyboard.pressAndRelease(it)
                    delay(duration)
                    Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
                    delay(duration)
                }
            )
        }
    }

    suspend fun jeoju() {
        tabTab()
        while (true) {
            currentCoroutineContext().ensureActive()
            Keyboard.pressAndRelease(JEOJU)
            Keyboard.pressAndRelease(latestDirection)
            Keyboard.pressAndRelease(KeyEvent.VK_ENTER, DELAY)
        }
    }

    suspend fun mabee() {
        healMe()
        while (true) {
            currentCoroutineContext().ensureActive()
            Keyboard.pressAndRelease(MABEE)
            Keyboard.pressAndRelease(latestDirection)
            Keyboard.pressAndRelease(KeyEvent.VK_ENTER, DELAY)
        }
    }

    suspend fun julmang() {
        tabTab()
        while (true) {
            currentCoroutineContext().ensureActive()
            Keyboard.pressAndRelease(JULMANG)
            Keyboard.pressAndRelease(latestDirection)
            Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
        }
    }

    suspend fun maagi() {
        executeAlphabetMagic(
            Triple(MAGII, false, false)
        )
    }

    suspend fun samme() {
        executeAlphabetMagic(
            Triple(SAMME, false, false)
        )
    }

    suspend fun gongjeung() {
        Keyboard.pressAndRelease(GONGJEUNG)
    }

    suspend fun hondon() {
        executeAlphabetMagic(Triple(HONDON, false, false))
    }

    private suspend fun bomu(focusMe: Boolean) {
        executeAlphabetMagic(
            Triple(BOHO, focusMe, true),
            Triple(MUJANG, false, true),
        )
    }

    private suspend fun healMe() {
        focusMe(
            keyEvent = HEAL,
            action = {
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
            }
        )
    }
    private suspend fun heal(time: Int) {
        Keyboard.pressKeyRepeatedly(
            keyEvent = HEAL,
            time = time,
            delay = DELAY
        )
    }

    suspend fun tabTab() {
        val duration = 30L
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, duration)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME, duration)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, duration)
    }

    private suspend inline fun focusMe(
        keyEvent: Int,
        crossinline action: suspend () -> Unit,
    ) {
        Keyboard.pressAndRelease(keyEvent)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME)
        action.invoke()
    }

    private suspend fun executeAlphabetMagic(vararg args: Triple<Int, Boolean, Boolean>) {
        args.forEach { arg ->
            val (magic, forMe, enter) = arg

            Keyboard.press(KeyEvent.VK_SHIFT)
            delay(DELAY)

            Keyboard.pressAndRelease(KeyEvent.VK_Z)
            Keyboard.release(KeyEvent.VK_SHIFT)
            delay(DELAY)

            Keyboard.pressAndRelease(magic)
            if(forMe) {
                delay(DELAY)
                Keyboard.pressAndRelease(KeyEvent.VK_HOME)
            }
            
            if(enter) {
                delay(DELAY)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
            }
        }
    }

    companion object {
        // a(1), b(2), c(3) 비움,
        // d(4): 활력,
        // e(5): 공증,
        // f(6): 마비,
        // g(7): 절망,
        // h(8): 저주,
        // i(9): 기원,
        // j(0): 헬파,
        private const val DELAY = 60L
        private const val HELLFIRE = KeyEvent.VK_0
        private const val MABEE = KeyEvent.VK_6
        private const val JULMANG = KeyEvent.VK_7
        private const val JEOJU = KeyEvent.VK_8
        private const val HEAL = KeyEvent.VK_9
        private const val SAMME = KeyEvent.VK_K
        private const val GONGJEUNG = KeyEvent.VK_5
        private const val BOHO = KeyEvent.VK_M
        private const val MUJANG = KeyEvent.VK_N
        private const val MAGII = KeyEvent.VK_O
        private const val HONDON = KeyEvent.VK_P
    }
}