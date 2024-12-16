package follower.macro

import common.Keyboard
import follower.Follower
import follower.ocr.TextDetecter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.awt.Rectangle
import java.awt.event.KeyEvent

object FollowerMacro {
    private val scope = CoroutineScope(SupervisorJob())

    private var healJob: Job? = null
    private var honmaJob: Job? = null
    private val failureTargets = listOf("실패", "심패")

    private var buffRect = Rectangle()
    private var magicRect = Rectangle()

    private val textDetector = TextDetecter()

    fun init(follower: Follower) {
        scope.launch {
            follower.uiState.collectLatest {
                buffRect = it.buffStateRect
                magicRect = it.magicRect
            }
        }
    }

    suspend fun gongju() {
        cancelAll()
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)

        Keyboard.pressAndRelease(KeyEvent.VK_TAB, delay = 70)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB)

        Keyboard.pressAndRelease(KeyEvent.VK_8)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)

        gongJeung()
        heal()
    }

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
            Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
            Keyboard.pressAndRelease(KeyEvent.VK_TAB, delay = 100)
            Keyboard.pressAndRelease(KeyEvent.VK_TAB)

            var toggle = 1
            while (isActive) {
                repeat(4) {
                    Keyboard.pressAndRelease(KeyEvent.VK_1)
                }

                if(toggle % 4 == 0) {
                    gongJeung(false)
                    toggle = 1
                } else {
                    toggle += 1
                }

                checkBuff()

                repeat(4) {
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
            text = textDetector.detectString(magicRect)
            println("@@@ gongjeung: $text")

            when {
                failureTargets.contains(text) -> continue
                text.contains("마력") -> {
                    Keyboard.pressAndRelease(KeyEvent.VK_U)
                    Keyboard.pressAndRelease(KeyEvent.VK_U)
                }
                text.contains("귀신") -> dead()
                text.contains("공력") -> break
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
        } while (textDetector.detect(failureTargets, magicRect))
    }

    suspend fun bomu(cancel: Boolean = true) {
        if(cancel) cancelAll()

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

    private suspend fun checkBuff() {
        val text = textDetector.detectString(buffRect)
        println("checkBuff: $text")
        if(!text.contains("보호") || !text.contains("무장")) {
            bomu(false)
        }

        if(!text.contains("금강")) {
            healMe(false)

            Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
            Keyboard.pressAndRelease(KeyEvent.VK_TAB, 100)
            Keyboard.pressAndRelease(KeyEvent.VK_TAB)

            invincible(false)
        }
    }

    private suspend fun maybeFailure() {
        val text = textDetector.detectString(magicRect)
        println("#### maybeFailure: $text")
        when {
            text.contains("귀신") -> {
                dead()
                return
            }
            text.contains("마력") -> {
                gongJeung(false)
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

        gongJeung(false)
        invincible(false)

    }
}