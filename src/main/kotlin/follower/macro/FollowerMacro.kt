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

    private var gongjeungJob: Job? = null
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

        tabTab()

        Keyboard.pressAndRelease(KeyEvent.VK_8)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)

        gongJeung()
        heal()
    }
    private suspend fun tabTab(){
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        delay(20)

        Keyboard.pressAndRelease(KeyEvent.VK_TAB)
        delay(20)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB)
    }

    suspend fun healMe(cancel: Boolean = true) { // BACK_QUOTE
        if(cancel) cancelAll()

        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        Keyboard.pressAndRelease(KeyEvent.VK_1)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
    }

    suspend fun heal() { // F1
        cancelAll()
        healJob = scope.launch {
            tabTab()

            var counter = 1
            while (isActive) {
                Keyboard.pressKeyRepeatedly(KeyEvent.VK_1, 4)

                if (counter % 3 == 0) {
                    gongJeung()
                    healMe(false)
                    tabTab()
                }
                counter = (counter % 3) + 1

                checkBuff()
                Keyboard.pressKeyRepeatedly(KeyEvent.VK_1, 4)
                maybeFailure()
            }
        }
    }

    suspend fun gongJeung() = withContext(Dispatchers.IO) { // F2
        var text: String
        while (isActive) {
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

        tabTab()
        Keyboard.pressAndRelease(KeyEvent.VK_6)
        Keyboard.pressAndRelease(KeyEvent.VK_7)
    }

    suspend fun cancelAll() {
        healJob?.cancel()
        healJob = null

        honmaJob?.cancel()
        honmaJob = null

        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        delay(20)
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        delay(20)
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
    }

    private suspend fun checkBuff() {
        Keyboard.pressAndRelease(KeyEvent.VK_S)
        val text = textDetector.detectString(buffRect)
        println("checkBuff: $text")
        if(!text.contains("보호") || !text.contains("무장")) {
            bomu(false)
        }

        if(!text.contains("금강")) {
            invincible(false)
        }
    }

    private suspend fun maybeFailure() {
        val text = textDetector.detectString(magicRect)
        println("#### maybeFailure: $text")
        when {
            text.contains("귀신") -> {
                dead()
            }
            text.contains("마력") -> {
                gongJeung()
                healMe(false)

                tabTab()
            }
            text.contains("다른") -> {
                Keyboard.pressAndRelease(KeyEvent.VK_0)
                bomu(false)
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

        gongJeung()
        invincible(false)
    }
}