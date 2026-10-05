package jusulsa.usecase

import jusulsa.engine.MacroUseCase
import jusulsa.skill.RateGroup
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.Target
import ocr.OcrStateHolder

/**
 * 체력이 [healBelow] 아래면 자힐. 게임은 1초에 앞의 3번만 받는다(RateGroup.HEAL).
 * 평타·첨이 밀리지 않게 한 차례에 한 번만 쓴다. 사이에 다른 마법이 대상을 바꾸므로 매번 HOME으로 나를 잡는다.
 * 체력 막대를 못 읽으면 아무것도 하지 않는다.
 */
class HealUseCase(
    private val healBelow: Int = HEAL_BELOW_PERCENT,
    override val name: String = "자힐",
    private val ocr: OcrStateHolder = OcrStateHolder,
    private val now: () -> Long = System::currentTimeMillis,
) : MacroUseCase {

    fun needsHeal(): Boolean {
        val hp = ocr.state.value.freshVitals(now())?.hpPercent ?: return false
        return hp < healBelow
    }

    /** 체력이 부족하고 이번 1초에 힐이 남아 있으면 */
    override fun isReady(now: Long) =
        needsHeal() && SkillCaster.remaining(RateGroup.HEAL) > 0 && SkillCaster.readyIn(Skill.HEAL) == 0L

    override suspend fun execute() {
        SkillCaster.tryCast(Skill.HEAL, Target.Me)
    }

    companion object {
        const val HEAL_BELOW_PERCENT = 90
        /** 이 아래면 첨첨보다 먼저 힐한다 */
        const val URGENT_BELOW_PERCENT = 50
    }
}
