package jusulsa.usecase

import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.Target
import ocr.OcrStateHolder
import ocr.model.TimerRegion

/**
 * 삼매진화. 체력이 [FULL_HP_PERCENT]% 이상이고 쿨타임 박스에 삼매진화가 없을 때만 나를 기준으로 쓴다.
 * 체력 막대나 쿨타임 박스를 못 읽으면 쓰지 않는다.
 */
class SammeUseCase(
    private val ocr: OcrStateHolder = OcrStateHolder,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var lastCastAt: Long? = null

    fun canCast(): Boolean {
        val time = now()
        // 쓴 직후에는 쿨타임 박스에 아직 안 잡힌다
        lastCastAt?.let { if (time - it < RECAST_GUARD_MILLIS) return false }

        val hp = ocr.state.value.freshVitals(time)?.hpPercent ?: return false
        if (hp < FULL_HP_PERCENT) return false

        val cooldown = ocr.state.value.fresh(TimerRegion.COOLDOWN, time) ?: return false
        val remaining = cooldown.find(NAME)?.remainingSeconds(time) ?: return true
        return remaining <= 0
    }

    /** 쓸 수 있으면 나를 기준으로 삼매진화를 쓴다. 썼으면 true */
    suspend operator fun invoke(): Boolean {
        if (!canCast()) return false
        SkillCaster.cast(Skill.SAMME, Target.Me)
        lastCastAt = now()
        return true
    }

    companion object {
        const val NAME = "삼매진화"
        /** 자힐 기준(HealUseCase.HEAL_BELOW_PERCENT)과 같게 둬야 힐 뒤에 삼매가 나간다 */
        const val FULL_HP_PERCENT = 90
        const val RECAST_GUARD_MILLIS = 5_000L
    }
}
