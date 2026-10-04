package jusulsa

import common.robot.Keyboard
import jusulsa.skill.*
import jusulsa.skill.SkillInput.focusMe
import jusulsa.skill.SkillInput.selectAlphabetMagic
import jusulsa.usecase.BomuUseCase
import jusulsa.usecase.MagiUseCase
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

class MacroDetailAction2(
    private val bomu: BomuUseCase = BomuUseCase(),
    private val magi: MagiUseCase = MagiUseCase(),
) {
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

        bomu()

        tabTab()
        while (true) {
            currentCoroutineContext().ensureActive()
            // 보무를 걸면 대상이 나로 바뀌므로 다시 탭탭으로 잡는다
            if (bomu()) tabTab()
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
//            Keyboard.pressAndRelease(KeyEvent.VK_ENTER, SKILL_DELAY)
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
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER, SKILL_DELAY)
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
        magi.cast()
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

        bomu()

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
        // 보무가 끊기기 전에, 마기지체는 쿨이 돌 때마다 건다
        launch {
            while (isActive) {
                bomu()
                magi()
                delay(1.seconds)
            }
        }
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
            delay = SKILL_DELAY
        )
    }

    suspend fun tabTab() = Keyboard.atomic {
        val duration = 30L
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, duration)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME, duration)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, duration)
    }

    companion object {
        private val DIRECTIONS = listOf(
            KeyEvent.VK_UP,
            KeyEvent.VK_LEFT,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_RIGHT
        )
    }
}