package jusulsa.usecase

import detector.DetectionStateHolder
import jusulsa.engine.MacroUseCase
import jusulsa.engine.ReasonLog
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.Target
import ocr.OcrStateHolder
import ocr.model.TimerRegion

/**
 * 삼매진화. 쿨타임 박스에 삼매진화가 없을 때 나를 기준으로 쓴다.
 * 쿨타임 박스를 못 읽으면 쓰지 않는다.
 * 몹 탐지가 켜져 있으면 삼매진화는 [FiveCrossUseCase]가 5매각에 쓰므로 여기서는 쓰지 않는다.
 */
class SammeUseCase(
    private val detecting: (now: Long) -> Boolean = { DetectionStateHolder.state.value?.isFresh(it) == true },
    private val ocr: OcrStateHolder = OcrStateHolder,
    private val now: () -> Long = System::currentTimeMillis,
) : MacroUseCase {
    override val name = "삼매진화"
    private var lastCastAt: Long? = null
    private val reason = ReasonLog("SammeUseCase")

    override fun isReady(now: Long) = canCast()

    fun canCast(): Boolean {
        val time = now()
        if (detecting(time)) {
            reason.log("몹 탐지 중이라 5매각에 맡김")
            return false
        }
        // 쓴 직후에는 쿨타임 박스에 아직 안 잡힌다
        lastCastAt?.let { 
            if (time - it < RECAST_GUARD_MILLIS) {
                reason.log("방어: 최근 5초 이내에 이미 사용함")
                return false 
            }
        }

        val cooldown = ocr.state.value.fresh(TimerRegion.COOLDOWN, time)
        if (cooldown == null) {
            reason.log("방어: TimerRegion.COOLDOWN(쿨타임 박스) 읽기 실패, 미설정 또는 5초 이상 지연됨")
            return false
        }
        
        val remaining = cooldown.find(NAME)?.remainingSeconds(time)
        if (remaining != null && remaining > 0) {
            reason.log("방어: 쿨타임 박스에 $NAME $remaining 초 남음으로 인식됨")
            return false
        }
        
        reason.log("조건 모두 통과! 삼매진화 시전 준비 완료")
        return true
    }

    /** 나를 기준으로 삼매진화를 쓴다 */
    override suspend fun execute() {
        if (SkillCaster.tryCast(Skill.SAMME, Target.Me)) lastCastAt = now()
    }

    companion object {
        const val NAME = "삼매진화"
        const val RECAST_GUARD_MILLIS = 5_000L
    }
}
