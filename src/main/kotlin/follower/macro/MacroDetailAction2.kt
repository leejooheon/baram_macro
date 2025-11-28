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

    fun changeDirection(event: Int) {
        latestDirection = event
    }

    suspend fun hellfire() {
        Keyboard.pressAndRelease(JEOJU)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME, DELAY)
        Keyboard.pressAndRelease(latestDirection)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
        Keyboard.pressAndRelease(HELLFIRE)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
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

    suspend fun jeoju() {
        tabTab()
        var count = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            Keyboard.pressAndRelease(JEOJU)
            Keyboard.pressAndRelease(latestDirection)
            Keyboard.pressAndRelease(KeyEvent.VK_ENTER, DELAY)
            if (++count % 8 == 0) delay(1.seconds)
        }
    }

    suspend fun mabee() {
        healMe()
        var count = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            Keyboard.pressAndRelease(MABEE)
            Keyboard.pressAndRelease(latestDirection)
            Keyboard.pressAndRelease(KeyEvent.VK_ENTER, DELAY)
            if (++count % 8 == 0) delay(1.seconds)
        }
    }

    suspend fun julmang() {
        tabTab()
        var count = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            Keyboard.pressAndRelease(JULMANG)
            Keyboard.pressAndRelease(latestDirection)
            Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
            if (++count % 8 == 0) delay(1.seconds)
        }
    }

    suspend fun maagi() {
        executeAlphabetMagic(
            Triple(KeyEvent.VK_B, false, false)
        )
    }

    suspend fun samme() {
        Keyboard.pressAndRelease(SAMME) // 삼매진화
    }

    suspend fun gongjeung() {
        Keyboard.pressAndRelease(GONGJEUNG)
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

    private suspend fun escape() {
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        delay(66)
    }

    suspend fun tabTab() {
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, 30)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME, 30)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, 30)
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

    private suspend fun executeAlphabetMagic(vararg args: Triple<Int, Boolean, Boolean>) {
        args.forEach { arg ->
            val (magic, forMe, enter) = arg

            Keyboard.press(KeyEvent.VK_SHIFT)
            delay(DELAY)

            Keyboard.pressAndRelease(KeyEvent.VK_Z)
            Keyboard.release(KeyEvent.VK_SHIFT)
            delay(DELAY)

            Keyboard.pressAndRelease(magic)
            delay(DELAY)

            if(forMe) {
                Keyboard.pressAndRelease(KeyEvent.VK_HOME)
                delay(DELAY)
            }
            
            if(enter) {
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
            }
        }
    }

    companion object {
        private const val DELAY = 60L
        const val HELLFIRE = KeyEvent.VK_1
        const val GONGJEUNG = KeyEvent.VK_2
        const val MABEE = KeyEvent.VK_3
        const val HWALRYUCK = KeyEvent.VK_4
        const val CHUM1 = KeyEvent.VK_5
        const val JULMANG = KeyEvent.VK_6
        const val JUNGDOK = KeyEvent.VK_7
        const val JEOJU = KeyEvent.VK_8
        const val HEAL = KeyEvent.VK_9
        const val SAMME = KeyEvent.VK_0
        private const val BOHO = KeyEvent.VK_G
        private const val MUJANG = KeyEvent.VK_H
    }
}