package jusulsa.usecase

import common.robot.UserInput
import jusulsa.engine.MacroUseCase
import jusulsa.skill.RateGroup
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.Target
import ocr.OcrStateHolder

/**
 * 체력이 [HEAL_BELOW_PERCENT] 아래면 자힐. 게임은 1초에 앞의 3번만 받으니, 쓸 수 있는 만큼 한 번에 몰아서 쓴다.
 * 첫 번째만 HOME으로 나를 잡고 나머지는 직전 대상(나)에게 쓴다. 대상이 나로 바뀌는 횟수가 줄어 저주 방향 잡기가 덜 흔들린다.
 * 체력 막대를 못 읽으면 아무것도 하지 않는다.
 */
class HealUseCase(
    private val ocr: OcrStateHolder = OcrStateHolder,
    private val now: () -> Long = System::currentTimeMillis,
) : MacroUseCase {
    override val name = "자힐"

    fun needsHeal(): Boolean {
        val hp = ocr.state.value.freshVitals(now())?.hpPercent ?: return false
        return hp < HEAL_BELOW_PERCENT
    }

    /** 체력이 부족하고 이번 1초에 힐이 남아 있으면 */
    override fun isReady(now: Long) =
        needsHeal() && SkillCaster.remaining(RateGroup.HEAL) > 0 && SkillCaster.readyIn(Skill.HEAL) == 0L

    /** 남은 만큼 몰아서 쓴다. 첫 번째만 나를 잡고 나머지는 직전 대상(나)에게 */
    override suspend fun execute() {
        val count = SkillCaster.remaining(RateGroup.HEAL)
        if (!SkillCaster.tryCast(Skill.HEAL, Target.Me)) return
        repeat(count - 1) {
            if (UserInput.isMoving()) return
            SkillCaster.tryCast(Skill.HEAL)
        }
    }

    companion object {
        const val HEAL_BELOW_PERCENT = 90
    }
}
