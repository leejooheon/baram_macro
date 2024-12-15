package follower

import common.Keyboard
import kotlinx.coroutines.*
import java.awt.event.KeyEvent

object FollowerMacro {
    private val scope = CoroutineScope(SupervisorJob())

    private var healJob: Job? = null
    private var honmaJob: Job? = null

    suspend fun healMe() { // BACK_QUOTE
        Keyboard.event(KeyEvent.VK_ESCAPE)
        Keyboard.event(KeyEvent.VK_HOME)
        Keyboard.event(KeyEvent.VK_1)
        Keyboard.event(KeyEvent.VK_ENTER)
    }

    suspend fun heal() { // F1
        cancelAll()

        healJob = scope.launch {
            Keyboard.event(KeyEvent.VK_ESCAPE)
            Keyboard.event(KeyEvent.VK_TAB)
            Keyboard.event(KeyEvent.VK_TAB)

            while (isActive) {
                Keyboard.event(
                    keyEvent = KeyEvent.VK_1, // 힐
                    delay = 400
                )
                Keyboard.event(KeyEvent.VK_2) // 공증
                Keyboard.event(KeyEvent.VK_4) // 금강불체
            }
        }
    }

    suspend fun gongJeung() { // F2
        Keyboard.event(KeyEvent.VK_U)
        Keyboard.event(KeyEvent.VK_U)
        Keyboard.event(KeyEvent.VK_2)
    }

    suspend fun honmasul() { // F3
        cancelAll()
        honmaJob = scope.launch {
            Keyboard.event(KeyEvent.VK_ESCAPE)
            while (isActive) {
                Keyboard.event(KeyEvent.VK_5)
                Keyboard.event(KeyEvent.VK_UP)
                Keyboard.event(KeyEvent.VK_ENTER)
            }
        }
    }

    suspend fun invincible() { // F4
        Keyboard.event(KeyEvent.VK_4)
    }

    suspend fun bomu() {
        Keyboard.event(KeyEvent.VK_ESCAPE)
        Keyboard.event(KeyEvent.VK_HOME)
        Keyboard.event(KeyEvent.VK_6)
        Keyboard.event(KeyEvent.VK_7)

        Keyboard.event(KeyEvent.VK_TAB)
        Keyboard.event(KeyEvent.VK_TAB)
        Keyboard.event(KeyEvent.VK_6)
        Keyboard.event(KeyEvent.VK_7)
    }

    suspend fun cancelAll() {
        Keyboard.event(KeyEvent.VK_ESCAPE)

        healJob?.cancel()
        healJob = null

        honmaJob?.cancel()
        honmaJob = null
    }
}