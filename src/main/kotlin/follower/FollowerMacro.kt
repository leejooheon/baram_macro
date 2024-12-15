package follower

import common.Keyboard
import kotlinx.coroutines.*
import java.awt.event.KeyEvent

object FollowerMacro {
    private val scope = CoroutineScope(SupervisorJob())

    private var healJob: Job? = null
    private var honmaJob: Job? = null

    suspend fun healMe() { // BACK_QUOTE
        cancelAll()

        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME)
        Keyboard.pressAndRelease(KeyEvent.VK_1)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
    }

    suspend fun heal() { // F1
        cancelAll()

        healJob = scope.launch {
            Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
            Keyboard.pressAndRelease(KeyEvent.VK_TAB)
            Keyboard.pressAndRelease(KeyEvent.VK_TAB)

            while (isActive) {
                Keyboard.pressAndRelease(
                    keyEvent = KeyEvent.VK_1, // 힐
                    delay = 400
                )
                Keyboard.pressAndRelease(KeyEvent.VK_2) // 공증
                Keyboard.pressAndRelease(KeyEvent.VK_4) // 금강불체
            }
        }
    }

    suspend fun gongJeung() { // F2
        cancelAll()

        Keyboard.pressAndRelease(KeyEvent.VK_U)
        Keyboard.pressAndRelease(KeyEvent.VK_U)
        Keyboard.pressAndRelease(KeyEvent.VK_2)
    }

    suspend fun honmasul() { // F3
        cancelAll()
        honmaJob = scope.launch {
            Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
            while (isActive) {
                Keyboard.pressAndRelease(KeyEvent.VK_5)
                Keyboard.pressAndRelease(KeyEvent.VK_UP)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
            }
        }
    }

    suspend fun invincible() { // F4
        cancelAll()

        Keyboard.pressAndRelease(KeyEvent.VK_4)
    }

    suspend fun bomu() {
        cancelAll()

        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME)
        Keyboard.pressAndRelease(KeyEvent.VK_6)
        Keyboard.pressAndRelease(KeyEvent.VK_7)

        Keyboard.pressAndRelease(KeyEvent.VK_TAB)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB)
        Keyboard.pressAndRelease(KeyEvent.VK_6)
        Keyboard.pressAndRelease(KeyEvent.VK_7)
    }

    suspend fun cancelAll() {
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)

        healJob?.cancel()
        healJob = null

        honmaJob?.cancel()
        honmaJob = null
    }
}