package jusulsa.usecase

import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.Target
import kotlinx.coroutines.withTimeoutOrNull
import ocr.OcrStateHolder

/**
 * 체력이 떨어지면 자힐. [HEAL_BELOW_PERCENT] 아래로 내려가면 [HEAL_UNTIL_PERCENT]까지 채운다.
 * 체력 막대를 못 읽으면 아무것도 하지 않는다.
 */
class HealUseCase(
    private val ocr: OcrStateHolder = OcrStateHolder,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun needsHeal(): Boolean {
        val hp = ocr.state.value.freshVitals(now())?.hpPercent ?: return false
        return hp < HEAL_BELOW_PERCENT
    }

    /** 체력이 부족하면 자힐한다. 했으면 true */
    suspend operator fun invoke(): Boolean {
        if (!needsHeal()) return false
        healUntil(HEAL_UNTIL_PERCENT)
        return true
    }

    /** 체력이 percent가 될 때까지 자힐. 초당 3번은 SkillCaster가 맞추고, 대상이 바뀌었을 수 있어 매번 나를 잡는다 */
    suspend fun healUntil(percent: Int) {
        withTimeoutOrNull(HEAL_MAX_MILLIS) {
            do {
                SkillCaster.cast(Skill.HEAL, Target.Me)
                val hp = ocr.state.value.freshVitals(now())?.hpPercent
            } while (hp == null || hp < percent)
        }
    }

    companion object {
        const val HEAL_BELOW_PERCENT = 70
        const val HEAL_UNTIL_PERCENT = 90
        /** 체력을 못 읽거나 잘 안 차도 이 시간이 지나면 멈춘다 */
        const val HEAL_MAX_MILLIS = 5_000L
    }
}
