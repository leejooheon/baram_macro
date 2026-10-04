package follower.macro

import common.robot.Keyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.event.KeyEvent
import kotlin.time.Duration
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
        focusMe(
            keyEvent = HEAL,
            action = {
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
                heal(2)
            }
        )

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
//            Keyboard.pressAndRelease(latestDirection)
//            Keyboard.pressAndRelease(KeyEvent.VK_ENTER, DELAY)
        }
    }
    private suspend fun jeoju2(direction: Int, duration: Duration) {
        val endTime = System.currentTimeMillis() + duration.inWholeMilliseconds
        while (System.currentTimeMillis() < endTime) {
            currentCoroutineContext().ensureActive()
            Keyboard.atomic {
                Keyboard.pressAndRelease(JEOJU)
                Keyboard.pressAndRelease(direction)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
            }
        }
    }

    suspend fun mabee() {
        while (true) {
            currentCoroutineContext().ensureActive()
            Keyboard.atomic {
                Keyboard.pressAndRelease(MABEE)
                Keyboard.pressAndRelease(latestDirection)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER, DELAY)
            }
        }
    }

    suspend fun julmang() {
        while (true) {
            currentCoroutineContext().ensureActive()
            Keyboard.atomic {
                Keyboard.pressAndRelease(JULMANG)
                Keyboard.pressAndRelease(latestDirection)
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
            }
        }
    }

    // 4방향으로 중독을 돌리고 사이사이 자힐
    private suspend fun jungDok(duration: Duration) {
        val endTime = System.currentTimeMillis() + duration.inWholeMilliseconds
        var cnt = 0
        var directionIndex = 0
        while (System.currentTimeMillis() < endTime) {
            currentCoroutineContext().ensureActive()
            if(cnt % 16 == 3) {
                directionIndex = (directionIndex + 1) % DIRECTIONS.size
            }
            if(cnt % 16 > 8) {
                Keyboard.atomic { healMe() }
                delay(300)
            } else {
                Keyboard.atomic {
                    selectAlphabetMagic(JUNGDOK, upper = true)
                    Keyboard.pressAndRelease(DIRECTIONS[directionIndex])
                    Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
                }
                delay(120)
            }
            cnt++
        }
    }

    suspend fun maagi() {
        executeAlphabetMagic(
            Triple(MAGII, false, false)
        )
    }

    suspend fun samme() {
        Keyboard.pressAndRelease(SAMME)
    }

    suspend fun gongjeung() {
        Keyboard.pressAndRelease(GONGJEUNG)
    }

    suspend fun hondon() {
        Keyboard.pressAndRelease(HONDON)
    }

    suspend fun chumChum() = withContext(Dispatchers.Default) {
        // 중독을 돌리는 동안 방향키가 눌리므로 시작 시점의 방향을 잡아둔다
        val direction = latestDirection

        bomu(true)
        bomuTime = System.currentTimeMillis()

        launch {
            while (isActive) {
                jeoju2(direction, 5.seconds)
                jungDok(30.seconds)
            }
        }
        launch {
            while (isActive) {
                Keyboard.atomic { selectAlphabetMagic(CHUM1) }
                delay(400)
                Keyboard.atomic { selectAlphabetMagic(CHUM2) }
                delay(400)
            }
        }
    }

    suspend fun bomu() {
        bomu(true)
        bomuTime = System.currentTimeMillis()
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

    suspend fun tabTab() = Keyboard.atomic {
        val duration = 30L
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, duration)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME, duration)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, duration)
    }

    private suspend inline fun focusMe(
        keyEvent: Int,
        crossinline action: suspend () -> Unit,
    ) = Keyboard.atomic {
        Keyboard.pressAndRelease(keyEvent)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME)
        action.invoke()
    }

    private suspend fun executeAlphabetMagic(vararg args: Triple<Int, Boolean, Boolean>) {
        args.forEach { arg ->
            val (magic, forMe, enter) = arg

            Keyboard.atomic {
                selectAlphabetMagic(magic)

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
    }

    // shift+z 후 알파벳, 대문자 칸은 shift를 누른 채로 알파벳까지 입력
    private suspend fun selectAlphabetMagic(magic: Int, upper: Boolean = false) = Keyboard.atomic {
        Keyboard.press(KeyEvent.VK_SHIFT)
        try {
            delay(DELAY)
            Keyboard.pressAndRelease(KeyEvent.VK_Z)
            if(upper) {
                delay(DELAY)
                Keyboard.pressAndRelease(magic)
            }
        } finally {
            Keyboard.release(KeyEvent.VK_SHIFT)
        }

        if(!upper) {
            delay(DELAY)
            Keyboard.pressAndRelease(magic)
        }
    }

    companion object {
        // a(1): 헬파이어,
        // b(2): 공력증강,
        // c(3): 마비,
        // d(4): 활력,
        // e(5): 혼돈,
        // f(6): 절망,
        // g(7): 저주,
        // h(8): 삼매진화,
        // i(9): 태양의기원,
        // j(0): 지폭지술,
        // 나머지는 shift+z + 알파벳
        // m: 보호, n: 무장, o: 마기지체, q: 극진뢰격참주'첨, r: 진뢰격참주'첨, G: 중독
        private const val DELAY = 60L
        private const val HELLFIRE = KeyEvent.VK_1
        private const val GONGJEUNG = KeyEvent.VK_2
        private const val MABEE = KeyEvent.VK_3
        private const val HONDON = KeyEvent.VK_5
        private const val JULMANG = KeyEvent.VK_6
        private const val JEOJU = KeyEvent.VK_7
        private const val SAMME = KeyEvent.VK_8
        private const val HEAL = KeyEvent.VK_9
        private const val BOHO = KeyEvent.VK_M
        private const val MUJANG = KeyEvent.VK_N
        private const val MAGII = KeyEvent.VK_O
        private const val CHUM1 = KeyEvent.VK_Q
        private const val CHUM2 = KeyEvent.VK_R
        private const val JUNGDOK = KeyEvent.VK_G // 대문자 G
        // A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z

        private val DIRECTIONS = listOf(
            KeyEvent.VK_UP,
            KeyEvent.VK_LEFT,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_RIGHT
        )
    }
}