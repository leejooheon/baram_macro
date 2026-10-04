package jusulsa.usecase

import common.robot.Keyboard
import jusulsa.engine.MacroUseCase
import jusulsa.skill.RateGroup
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.Target
import java.awt.event.KeyEvent

/** 극진뢰·진뢰 첨. 각자 최소 간격([Skill.minIntervalMillis])이 지나면 직전 대상에게 쓴다 */
class ChumUseCase : MacroUseCase {
    override val name = "첨"

    override fun isReady(now: Long) = next() != null

    override suspend fun execute() {
        val skill = next() ?: return
        SkillCaster.tryCast(skill)
    }

    private fun next() = CHUMS.firstOrNull { SkillCaster.readyIn(it) == 0L }

    companion object {
        private val CHUMS = listOf(Skill.CHUM1, Skill.CHUM2)
    }
}

/**
 * 저주는 내 사방 4칸에만 건다. 매번 HOME으로 나를 잡고 방향키를 눌러 바로 옆 몹에 건다.
 * 칸마다 [REFRESH_MILLIS]마다 다시 걸고, 가장 오래된 칸부터 건다.
 */
class CurseAroundUseCase(
    private val now: () -> Long = System::currentTimeMillis,
) : MacroUseCase {
    override val name = "저주(사방)"
    private val lastAt = DIRECTIONS.associateWith { 0L }.toMutableMap()

    override fun isReady(now: Long) =
        SkillCaster.readyIn(Skill.JEOJU) == 0L && lastAt.values.any { now - it >= REFRESH_MILLIS }

    override suspend fun execute() {
        val direction = lastAt.minBy { it.value }.key
        if (SkillCaster.tryCast(Skill.JEOJU, Target.Direction(direction, fromMe = true))) lastAt[direction] = now()
    }

    companion object {
        const val REFRESH_MILLIS = 5_000L
        private val DIRECTIONS = listOf(KeyEvent.VK_UP, KeyEvent.VK_LEFT, KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT)
    }
}

/**
 * 6번 칸(지금은 [Skill.JUNGDOK])을 맵 전체에 퍼뜨린다. 방향키는 지금 커서 위치에서 그 방향의 다음 몹으로 옮겨 가므로,
 * 같은 방향을 이어서 눌러야 멀리까지 퍼진다. 한 차례에 최대 [BURST]번을 이어서 걸고,
 * 한 방향에 [PER_DIRECTION]번을 건 뒤 다음 방향으로 넘어간다. 방향 순서는 캐릭터가 마지막으로 바라본 방향부터.
 * 저주 한도(RateGroup.CURSE)가 [CURSE_RESERVE]번 이하로 남았으면 사방 저주 몫으로 두고 쉰다.
 */
class DespairSpreadUseCase(
    private val facing: () -> Int = { KeyEvent.VK_LEFT },
) : MacroUseCase {
    override val name = "6번(맵 전체)"
    private var count = 0

    override fun isReady(now: Long) = canCast()

    override suspend fun execute() {
        repeat(BURST) {
            if (!canCast()) return
            // 이동키 때문에 취소됐으면 이번 차례는 끝낸다
            if (!SkillCaster.tryCast(Skill.JUNGDOK, Target.Direction(direction()))) return
            count++
        }
    }

    private fun canCast() =
        SkillCaster.readyIn(Skill.JUNGDOK) == 0L && SkillCaster.remaining(RateGroup.CURSE) > CURSE_RESERVE

    /** 바라보는 방향을 먼저, 그다음은 시계 반대 방향으로 돈다 */
    private fun direction(): Int {
        val first = DIRECTIONS.indexOf(facing()).coerceAtLeast(0)
        return DIRECTIONS[(first + count / PER_DIRECTION) % DIRECTIONS.size]
    }

    companion object {
        const val BURST = 3
        const val PER_DIRECTION = 6
        const val CURSE_RESERVE = 1
        private val DIRECTIONS = listOf(KeyEvent.VK_UP, KeyEvent.VK_LEFT, KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT)
    }
}

/** 평타. 예전 매크로처럼 스페이스바를 꾹 누르지 않고 [INTERVAL_MILLIS]마다 한 번씩 누른다 */
class BasicAttackUseCase(
    private val now: () -> Long = System::currentTimeMillis,
) : MacroUseCase {
    override val name = "평타"
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
