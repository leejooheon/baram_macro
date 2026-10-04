package jusulsa.usecase

import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.Target
import kotlinx.coroutines.withTimeoutOrNull
import ocr.OcrStateHolder
import ocr.model.TimerRegion

/**
 * 마력이 떨어졌을 때만 공력증강 + 자힐. 공증으로 마력을 채우면 체력이 깎이니 바로 힐로 체력을 채운다.
 * 체력/마력 막대를 못 읽으면 아무것도 하지 않는다.
 */
class ManaUseCase(
    private val ocr: OcrStateHolder = OcrStateHolder,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var lastGongjeungAt: Long? = null

    fun needsGongjeung(): Boolean {
        val time = now()
        // 공증 직후에는 막대가 아직 안 바뀌었을 수 있다
        lastGongjeungAt?.let { if (time - it < RECAST_GUARD_MILLIS) return false }

        val mp = ocr.state.value.freshVitals(time)?.mpPercent ?: return false
        if (mp > OcrStateHolder.MANA_LOW_PERCENT) return false

        // 쿨타임 박스에 공력증강이 보이면 아직 못 쓴다
        val cooldown = ocr.state.value.fresh(TimerRegion.COOLDOWN, time)
        val remaining = cooldown?.find(GONGJEUNG_NAME)?.remainingSeconds(time)
        return remaining == null || remaining <= 0
    }

    /** 마력이 부족하면 공증하고 체력을 채운다. 했으면 true */
    suspend operator fun invoke(): Boolean {
        if (!needsGongjeung()) return false

        SkillCaster.cast(Skill.GONGJEUNG)
        lastGongjeungAt = now()

        // 초당 3번은 SkillCaster가 맞춘다. 다른 매크로가 대상을 바꿀 수 있어서 매번 나를 잡는다
        withTimeoutOrNull(HEAL_MAX_MILLIS) {
            do {
                SkillCaster.cast(Skill.HEAL, Target.Me)
                val hp = ocr.state.value.freshVitals(now())?.hpPercent
            } while (hp == null || hp < HEAL_UNTIL_PERCENT)
        }
        return true
    }

    companion object {
        const val GONGJEUNG_NAME = "공력증강"
        /** 공증 뒤 체력이 이만큼 찰 때까지 자힐한다 */
        const val HEAL_UNTIL_PERCENT = 90
        /** 체력을 못 읽거나 잘 안 차도 이 시간이 지나면 멈춘다 */
        const val HEAL_MAX_MILLIS = 5_000L
        const val RECAST_GUARD_MILLIS = 3_000L
    }
}
