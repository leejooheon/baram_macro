package jusulsa

import common.robot.Keyboard
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster.cast
import jusulsa.skill.Target
import jusulsa.usecase.BomuUseCase
import jusulsa.usecase.HealUseCase
import jusulsa.usecase.MagiUseCase
import jusulsa.usecase.ManaUseCase
import jusulsa.usecase.SammeUseCase
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

/**
 * 주술사 매크로. 무엇을 어떤 순서로 쓸지만 정하고,
 * 초당 횟수, 최소 간격, 사용자 이동키 같은 규칙은 SkillCaster가 지킨다.
 */
class MacroDetailAction2(
    private val bomu: BomuUseCase = BomuUseCase(),
    private val magi: MagiUseCase = MagiUseCase(),
    private val selfHeal: HealUseCase = HealUseCase(),
    private val mana: ManaUseCase = ManaUseCase(),
    private val sammeUseCase: SammeUseCase = SammeUseCase(),
) {
    private var latestDirection: Int = KeyEvent.VK_LEFT

    fun onDirectionChanged(event: Int) {
        latestDirection = event
    }

    suspend fun hellfire() = Keyboard.atomic {
        cast(Skill.JEOJU, Target.Direction(latestDirection, fromMe = true))
        cast(Skill.HELLFIRE, Target.Confirm)
    }

    suspend fun heal() {
        cast(Skill.HEAL, Target.Me)
        repeat(2) { cast(Skill.HEAL) }

        bomu()

        tabTab()
        while (true) {
            currentCoroutineContext().ensureActive()
            // 보무를 걸면 대상이 나로 바뀌므로 다시 탭탭으로 잡는다
            if (bomu()) tabTab()
            // 초당 3번은 SkillCaster가 맞춘다
            cast(Skill.HEAL)
        }
    }

    suspend fun mabeAroundMe() {
        DIRECTIONS.forEach { cast(Skill.MABEE, Target.Direction(it, fromMe = true)) }
    }

    suspend fun jeoju() {
        tabTab()
        while (true) {
            currentCoroutineContext().ensureActive()
            cast(Skill.JEOJU)
        }
    }

    private suspend fun jeoju2(direction: Int, duration: Duration) {
        val endTime = System.currentTimeMillis() + duration.inWholeMilliseconds
        while (System.currentTimeMillis() < endTime) {
            currentCoroutineContext().ensureActive()
            cast(Skill.JEOJU, Target.Direction(direction))
        }
    }

    suspend fun mabee() {
        while (true) {
            currentCoroutineContext().ensureActive()
            cast(Skill.MABEE, Target.Direction(latestDirection))
        }
    }

    suspend fun julmang() {
        while (true) {
            currentCoroutineContext().ensureActive()
            cast(Skill.JULMANG, Target.Direction(latestDirection))
        }
    }

    // 4방향으로 중독을 돌린다. 자힐은 HealUseCase, ManaUseCase가 한다
    private suspend fun jungDok(duration: Duration) {
        val endTime = System.currentTimeMillis() + duration.inWholeMilliseconds
        var cnt = 0
        var directionIndex = 0
        while (System.currentTimeMillis() < endTime) {
            currentCoroutineContext().ensureActive()
            if(cnt > 0 && cnt % JUNGDOK_PER_DIRECTION == 0) {
                directionIndex = (directionIndex + 1) % DIRECTIONS.size
            }
            cast(Skill.JUNGDOK, Target.Direction(DIRECTIONS[directionIndex]))
            delay(120)
            cnt++
        }
    }

    suspend fun maagi() {
        magi.cast()
    }

    suspend fun samme() {
        cast(Skill.SAMME)
    }

    suspend fun gongjeung() {
        cast(Skill.GONGJEUNG)
    }

    suspend fun hondon() {
        cast(Skill.HONDON)
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
        // 극진뢰·진뢰 각각의 간격(400ms)은 SkillCaster가 맞춘다
        launch {
            while (isActive) {
                cast(Skill.CHUM1)
                cast(Skill.CHUM2)
            }
        }
        // 보무가 끊기기 전에, 마기지체는 쿨이 돌 때마다, 자힐은 체력이 떨어졌을 때, 공증+자힐은 마력이 떨어졌을 때,
        // 삼매진화는 체력이 가득 차고 쿨이 돌았을 때 나를 기준으로 쓴다
        launch {
            while (isActive) {
                bomu()
                magi()
                selfHeal()
                mana()
                sammeUseCase()
                delay(1.seconds)
            }
        }
    }

    suspend fun tabTab() = Keyboard.atomic {
        val duration = 30L
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, duration)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME, duration)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, duration)
    }

    companion object {
        /** 한 방향에 중독을 몇 번 걸고 다음 방향으로 넘어갈지 */
        private const val JUNGDOK_PER_DIRECTION = 9
        private val DIRECTIONS = listOf(
            KeyEvent.VK_UP,
            KeyEvent.VK_LEFT,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_RIGHT
        )
    }
}
