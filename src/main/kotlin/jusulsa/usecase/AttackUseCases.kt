package jusulsa.usecase

import common.robot.Keyboard
import jusulsa.engine.MacroUseCase
import jusulsa.skill.RateGroup
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.Target
import kotlinx.coroutines.delay
import java.awt.event.KeyEvent

/**
 * 극진뢰·진뢰 첨. 각자 최소 간격([Skill.minIntervalMillis])이 지나면 직전 대상에게 쓴다.
 * 자힐·삼매 등으로 대상이 나로 바뀌어 있으면, 첨이 나에게 가지 않게 바라보는 방향의 바로 옆 몹을 다시 잡아서 쓴다.
 */
class ChumUseCase(
    private val facing: () -> Int = { KeyEvent.VK_LEFT },
) : MacroUseCase {
    override val name = "첨"
    /** 자주 돌아서 5초 요약에만 남긴다 */
    override val logEachRun = false

    override fun isReady(now: Long) = next() != null

    override suspend fun execute() {
        val skill = next() ?: return
        val target = if (SkillCaster.targetIsMe) Target.Direction(facing(), fromMe = true) else Target.Current
        SkillCaster.tryCast(skill, target)
    }

    private fun next() = CHUMS.firstOrNull { SkillCaster.readyIn(it) == 0L }

    companion object {
        private val CHUMS = listOf(Skill.CHUM1, Skill.CHUM2)
    }
}

/**
 * 저주는 내 사방 4칸에만 건다. 4칸을 한 번에 빠르게 걸고, 다 걸고 나서 [REST_MILLIS] 쉰다.
 * 칸마다 HOME으로 나를 잡고 방향키를 눌러 바로 옆 몹에 건다.
 * 이동키나 한도 때문에 끊기면 남은 칸만 다음 차례에 이어서 걸고, 쉬는 시간은 다 건 뒤부터 센다.
 */
class CurseAroundUseCase(
    /** 한 바퀴 도중 마법 사이에 할 일 (평타) */
    private val afterCast: suspend () -> Unit = {},
    private val now: () -> Long = System::currentTimeMillis,
) : MacroUseCase {
    override val name = "저주(사방)"
    private var roundEndedAt = 0L
    private val pending = ArrayDeque<Int>()

    override fun isReady(now: Long): Boolean {
        if (pending.isEmpty() && now - roundEndedAt >= REST_MILLIS) pending.addAll(DIRECTIONS)
        return pending.isNotEmpty() && SkillCaster.readyIn(Skill.JEOJU) == 0L
    }

    override suspend fun execute() {
        while (pending.isNotEmpty()) {
            if (SkillCaster.readyIn(Skill.JEOJU) > 0L) return
            if (!SkillCaster.tryCast(Skill.JEOJU, Target.Direction(pending.first(), fromMe = true))) return
            pending.removeFirst()
            afterCast()
            // 예전 매크로(jeoju)의 간격
            delay(GAP_MILLIS)
        }
        roundEndedAt = now()
    }

    companion object {
        /** 한 바퀴를 다 건 뒤 쉬는 시간 */
        const val REST_MILLIS = 5_000L
        /** 마법 사이 간격 */
        const val GAP_MILLIS = 80L
        private val DIRECTIONS = listOf(KeyEvent.VK_UP, KeyEvent.VK_LEFT, KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT)
    }
}

/**
 * 6번 칸(지금은 [Skill.JUNGDOK])을 맵 전체에 한 바퀴 빠르게 퍼뜨리고, 다 건 뒤 [REST_MILLIS] 쉰다.
 * 한 차례는 바라보는 방향부터 네 방향으로, 방향마다 나를 기준으로 시작해 같은 방향키를 이어 눌러 [PER_DIRECTION]마리까지 건다.
 * 방향키는 지금 커서 위치에서 그 방향의 다음 몹으로 옮겨 가므로, 이어서 눌러야 멀리까지 퍼진다.
 * 저주 한도(초당 8번)에 걸리면 남은 것은 다음 차례에 이어서 걸고, 이동키로 끊긴 방향은 나를 기준으로 다시 시작한다.
 */
class DespairSpreadUseCase(
    private val facing: () -> Int = { KeyEvent.VK_LEFT },
    /** 한 바퀴 도중 마법 사이에 할 일 (평타) */
    private val afterCast: suspend () -> Unit = {},
) : MacroUseCase {
    override val name = "6번(맵 전체)"
    private var roundEndedAt = 0L
    /** 이번 바퀴에 남은 (방향, 그 방향에서 몇 번째) */
    private val pending = ArrayDeque<Pair<Int, Int>>()

    override fun isReady(now: Long): Boolean {
        if (pending.isEmpty() && now - roundEndedAt >= REST_MILLIS) startRound()
        return pending.isNotEmpty() && canCast()
    }

    override suspend fun execute() {
        while (pending.isNotEmpty()) {
            if (!canCast()) return
            val (direction, step) = pending.first()
            // 방향마다 처음은 나를 기준으로 잡아야 다른 마법이 옮겨 놓은 커서와 상관없이 퍼진다
            if (!SkillCaster.tryCast(Skill.JUNGDOK, Target.Direction(direction, fromMe = step == 0))) {
                restartDirection(direction)
                return
            }
            pending.removeFirst()
            afterCast()
            // 예전 매크로(jungdok)의 간격
            delay(GAP_MILLIS)
        }
        roundEndedAt = System.currentTimeMillis()
    }

    private fun canCast() =
        SkillCaster.readyIn(Skill.JUNGDOK) == 0L && SkillCaster.remaining(RateGroup.CURSE) > CURSE_RESERVE

    /** 바라보는 방향을 먼저, 그다음은 시계 반대 방향으로 */
    private fun startRound() {
        val first = DIRECTIONS.indexOf(facing()).coerceAtLeast(0)
        repeat(DIRECTIONS.size) { i ->
            val direction = DIRECTIONS[(first + i) % DIRECTIONS.size]
            repeat(PER_DIRECTION) { step -> pending.addLast(direction to step) }
        }
    }

    /** 끊긴 방향의 남은 칸을 나를 기준으로 다시 시작하게 바꾼다 */
    private fun restartDirection(direction: Int) {
        val left = pending.count { it.first == direction }
        pending.removeAll { it.first == direction }
        repeat(left) { step -> pending.addFirst(direction to (left - 1 - step)) }
    }

    companion object {
        /** 한 바퀴를 다 건 뒤 쉬는 시간 */
        const val REST_MILLIS = 10_000L
        /** 마법 사이 간격 */
        const val GAP_MILLIS = 120L
        const val PER_DIRECTION = 4
        const val CURSE_RESERVE = 1
        private val DIRECTIONS = listOf(KeyEvent.VK_UP, KeyEvent.VK_LEFT, KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT)
    }
}

/**
 * 평타. 예전 매크로처럼 스페이스바를 꾹 누르지 않고 [INTERVAL_MILLIS]마다 한 번씩 누른다.
 * 키 하나라 금방 끝나므로 공격 순환에서 차례를 기다리지 않게 우선 목록에 둔다.
 * 예전 매크로처럼 마법 사이에 섞는다: 엔진이 마법 하나를 쓸 때마다 바로 뒤에 한 번 누른다(MacroEngine.between).
 */
class BasicAttackUseCase(
    private val now: () -> Long = System::currentTimeMillis,
) : MacroUseCase {
    override val name = "평타"
    override val logEachRun = false
    /** 키 하나라 공격에게 양보할 필요가 없다 */
    override val neverYield = true
    private var lastAt = 0L

    override fun isReady(now: Long) = now - lastAt >= INTERVAL_MILLIS

    override suspend fun execute() {
        Keyboard.pressAndRelease(KeyEvent.VK_SPACE)
        lastAt = now()
    }

    companion object {
        const val INTERVAL_MILLIS = 450L
    }
}
