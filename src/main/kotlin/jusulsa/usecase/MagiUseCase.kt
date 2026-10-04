package jusulsa.usecase

import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import ocr.OcrStateHolder
import ocr.model.TimerRegion

/**
 * 마기지체. 쿨타임이 지속시간보다 길어서, 버프가 남았는지가 아니라 쿨타임 박스에 마기지체가 없을 때만 건다.
 * 쿨타임 박스를 못 읽으면 쿨인지 알 수 없으니 걸지 않는다.
 */
class MagiUseCase(
    private val ocr: OcrStateHolder = OcrStateHolder,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var lastCastAt: Long? = null

    fun canCast(): Boolean {
        val time = now()
        lastCastAt?.let { if (time - it < RECAST_GUARD_MILLIS) return false }

        val cooldown = ocr.state.value.fresh(TimerRegion.COOLDOWN, time) ?: return false
        val entry = cooldown.find(NAME) ?: return true
        return (entry.remainingSeconds(time) ?: return false) <= 0
    }

    /** 쿨이 돌았으면 마기지체를 건다. 걸었으면 true */
    suspend operator fun invoke(): Boolean {
        if (!canCast()) return false
        cast()
        return true
    }

    /** 쿨타임과 상관없이 바로 건다 (단축키용) */
    suspend fun cast() {
        SkillCaster.cast(Skill.MAGII)
        lastCastAt = now()
    }

    companion object {
        const val NAME = "마기지체"
        const val RECAST_GUARD_MILLIS = 5_000L
    }
}
