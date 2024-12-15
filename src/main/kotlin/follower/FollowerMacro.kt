package follower

import common.Keyboard
import follower.ocr.TextDetecter
import kotlinx.coroutines.*
import java.awt.event.KeyEvent

object FollowerMacro {
    private val scope = CoroutineScope(SupervisorJob())

    private var healJob: Job? = null
    private var honmaJob: Job? = null
    private lateinit var textDetector: TextDetecter
    private val failureTargets = listOf("실패", "심패")

    fun init(detecter: TextDetecter) {
        textDetector = detecter
    }
    // 2050, 1100, 200, 30
    suspend fun healMe(cancel: Boolean = true) { // BACK_QUOTE
        if(cancel) cancelAll()

        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        Keyboard.pressAndRelease(KeyEvent.VK_1)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
    }

    suspend fun heal() { // F1
        cancelAll()
        healJob = scope.launch {
            Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
            Keyboard.pressAndRelease(KeyEvent.VK_TAB, delay = 100)
            Keyboard.pressAndRelease(KeyEvent.VK_TAB)

            while (isActive) {
                repeat(4) {
                    Keyboard.pressAndRelease(KeyEvent.VK_1)
                }

                gongJeung(false)
                healMe(false)
                invincible(false)

                Keyboard.pressAndRelease(KeyEvent.VK_TAB, delay = 50)
                Keyboard.pressAndRelease(KeyEvent.VK_TAB)

                repeat(6) {
                    Keyboard.pressAndRelease(KeyEvent.VK_1)
                }
                Keyboard.pressAndRelease(KeyEvent.VK_0)

                maybeFailure()
            }
        }
    }

    suspend fun gongJeung(cancel: Boolean = true) = withContext(Dispatchers.IO) { // F2
        if(cancel) cancelAll()

        var text: String
        while (true) {
            Keyboard.pressAndRelease(KeyEvent.VK_2)
            text = textDetector.detectString()
            println("@@@ gongjeung: $text")

            when {
                 failureTargets.contains(text) -> {
                    println("@@@ gongjeung: 4561")
                    continue
                }
                text.contains("마력") -> {
                    println("@@@ gongjeung: 123123")
                    Keyboard.pressAndRelease(KeyEvent.VK_U)
                    Keyboard.pressAndRelease(KeyEvent.VK_U)
                }
                text.contains("공력") -> {
                    break
                }
            }
        }
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

    suspend fun invincible(cancel: Boolean = true) = withContext(Dispatchers.IO) { // F4
        if(cancel) cancelAll()

        do {
            Keyboard.pressAndRelease(KeyEvent.VK_4)
        } while (textDetector.detect(failureTargets))
    }

    suspend fun bomu() {
        cancelAll()

        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        Keyboard.pressAndRelease(KeyEvent.VK_6)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
        Keyboard.pressAndRelease(KeyEvent.VK_7)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)

        Keyboard.pressAndRelease(KeyEvent.VK_TAB, delay = 100)
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

    private suspend fun maybeFailure() {
        val text = textDetector.detectString()
        println("#### maybeFailure: $text")
        when {
            text.contains("마력") -> {
                gongJeung(false)
            }
            text.contains("귀신") -> {
                dead()
                healMe(false)
                invincible(false)
            }
        }
    }

    private suspend fun dead() {
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        Keyboard.pressAndRelease(KeyEvent.VK_0)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)

        Keyboard.pressAndRelease(KeyEvent.VK_1)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)

    }
}