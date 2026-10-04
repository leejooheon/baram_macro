package jusulsa

import common.robot.Keyboard
import common.robot.UserInput
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
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
import kotlinx.coroutines.withContext
import java.awt.event.KeyEvent

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

    /**
     * 첨첨. 키 입력은 한 번에 하나뿐이라 여러 루프가 락을 두고 다투지 않게 한 루프에서 우선순위대로 하나씩 고른다.
     * 고른 마법은 기다리지 않고 바로 쓸 수 있는 것만 쓰고, 아무것도 못 쓰면 잠깐 쉬었다 다시 고른다.
     *
     * 1. 사용자가 이동 중이면 아무것도 안 쓴다
     * 2. 체력이 낮으면 자힐, 마력이 낮으면 공증
     * 3. 보무, 마기지체, 삼매진화가 필요하면
     * 4. 극진뢰·진뢰 (각각 400ms 간격)
     * 5. 남는 시간에 저주 5초 -> 4방향 중독 30초 반복
     */
    suspend fun chumChum() = withContext(Dispatchers.Default) {
        // 중독을 돌리는 동안 방향키가 눌리므로 시작 시점의 방향을 잡아둔다
        val attack = AttackCycle(latestDirection)

        while (isActive) {
            val moving = UserInput.waitMillis()
            if (moving > 0) {
                delay(moving)
                continue
            }

            val casted = selfHeal() || mana() ||
                bomu() || magi() || sammeUseCase() ||
                chum() || attack.step()
            if (!casted) delay(IDLE_MILLIS)
        }
    }

    private suspend fun chum(): Boolean {
        val skill = CHUMS.firstOrNull { SkillCaster.readyIn(it) == 0L } ?: return false
        cast(skill)
        return true
    }

    /** 저주를 [JEOJU_MILLIS] 동안 걸고, 중독을 [JUNGDOK_MILLIS] 동안 4방향으로 돌리기를 반복한다 */
    private class AttackCycle(private val jeojuDirection: Int) {
        private var phaseStartedAt = System.currentTimeMillis()
        private var jungDokCount = 0

        private val jeojuPhase get() =
            (System.currentTimeMillis() - phaseStartedAt) % (JEOJU_MILLIS + JUNGDOK_MILLIS) < JEOJU_MILLIS

        /** 지금 쓸 수 있으면 하나 쓴다. 썼으면 true */
        suspend fun step(): Boolean {
            if (jeojuPhase) {
                jungDokCount = 0
                if (SkillCaster.readyIn(Skill.JEOJU) > 0) return false
                cast(Skill.JEOJU, Target.Direction(jeojuDirection))
            } else {
                if (SkillCaster.readyIn(Skill.JUNGDOK) > 0) return false
                val direction = DIRECTIONS[(jungDokCount / JUNGDOK_PER_DIRECTION) % DIRECTIONS.size]
                cast(Skill.JUNGDOK, Target.Direction(direction))
                jungDokCount++
            }
            return true
        }
    }

    suspend fun tabTab() = Keyboard.atomic {
        val duration = 30L
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, duration)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME, duration)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, duration)
    }

    companion object {
        /** 쓸 수 있는 마법이 없을 때 다시 고르기까지 쉬는 시간 */
        private const val IDLE_MILLIS = 20L
        private const val JEOJU_MILLIS = 5_000L
        private const val JUNGDOK_MILLIS = 30_000L
        private val CHUMS = listOf(Skill.CHUM1, Skill.CHUM2)
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
