package jusulsa.usecase

import common.robot.Keyboard
import jusulsa.engine.MacroUseCase
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
 * 한 방향으로 저주를 걸어 다음 몹으로 커서를 옮기고, 같은 몹에 바로 중독을 건다.
 * 방향키는 지금 커서 위치에서 그 방향의 다음 몹으로 옮겨 가므로, 같은 방향을 이어서 눌러야 멀리까지 퍼진다.
 * 한 방향에 [PER_DIRECTION]마리를 걸고 다음 방향으로 넘어간다. 저주는 초당 8번(RateGroup.CURSE)까지.
 */
class CursePoisonUseCase : MacroUseCase {
    override val name = "저주+중독"
    private var count = 0

    override fun isReady(now: Long) = SkillCaster.readyIn(Skill.JEOJU) == 0L

    override suspend fun execute() {
        val direction = DIRECTIONS[(count / PER_DIRECTION) % DIRECTIONS.size]
        // 저주가 이동키 때문에 취소됐으면 중독은 쓰지 않는다 (엉뚱한 대상에게 갈 수 있다)
        if (!SkillCaster.tryCast(Skill.JEOJU, Target.Direction(direction))) return
        SkillCaster.tryCast(Skill.JUNGDOK)
        count++
    }

    companion object {
        const val PER_DIRECTION = 5
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
