package jusulsa.usecase

import common.robot.UserInput
import jusulsa.engine.MacroUseCase
import jusulsa.skill.RateGroup
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.Target
import ocr.OcrStateHolder

/**
 * 체력이 [healBelow] 아래면 자힐. 게임은 1초에 앞의 3번만 받는다(RateGroup.HEAL).
 * 한 차례에 [BURST]번을 빠르게 몰아 쓰고(첫 번째만 HOME으로 나를 잡고 나머지는 직전 대상인 나에게),
 * 그 뒤 [REST_MILLIS] 동안은 쉬면서 다른 일에 차례를 넘긴다.
 * 체력 막대를 못 읽으면 아무것도 하지 않는다.
 */
class HealUseCase(
    private val healBelow: Int = HEAL_BELOW_PERCENT,
    override val name: String = "자힐",
    private val ocr: OcrStateHolder = OcrStateHolder,
    private val now: () -> Long = System::currentTimeMillis,
) : MacroUseCase {
    private var burstEndedAt = 0L

    fun needsHeal(): Boolean {
        val hp = ocr.state.value.freshVitals(now())?.hpPercent ?: return false
        return hp < healBelow
    }

    /** 체력이 부족하고 이번 1초에 힐이 남아 있으면 */
    override fun isReady(now: Long) =
        now - burstEndedAt >= REST_MILLIS && needsHeal() &&
            SkillCaster.remaining(RateGroup.HEAL) > 0 && SkillCaster.readyIn(Skill.HEAL) == 0L

    override suspend fun execute() {
        try {
            if (!SkillCaster.tryCast(Skill.HEAL, Target.Me)) return
            repeat(BURST - 1) {
                if (UserInput.isMoving()) return
                SkillCaster.tryCast(Skill.HEAL)
            }
        } finally {
            burstEndedAt = now()
        }
    }

    companion object {
        const val HEAL_BELOW_PERCENT = 90
        const val BURST = 3
        /** 몰아 쓴 뒤 쉬는 시간 */
        const val REST_MILLIS = 1_000L
    }
}
