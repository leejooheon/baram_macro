package jusulsa.usecase

import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.Target
import ocr.OcrStateHolder
import ocr.model.TimerRegion

/**
 * 삼매진화. 체력이 [MIN_HP_PERCENT]% 이상이고 쿨타임 박스에 삼매진화가 없을 때만 나를 기준으로 쓴다.
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
        lastCastAt?.let { 
            if (time - it < RECAST_GUARD_MILLIS) {
                println("[SammeUseCase] 방어: 최근 5초 이내에 이미 사용함")
                return false 
            }
        }

        val vitals = ocr.state.value.freshVitals(time)
        if (vitals == null) {
            println("[SammeUseCase] 방어: freshVitals(체력바 OCR) 읽기 실패 또는 지연됨")
            return false
        }
        val hp = vitals.hpPercent
        if (hp < MIN_HP_PERCENT) {
            println("[SammeUseCase] 방어: 체력이 $hp% 라서 시도 안 함 (기준: $MIN_HP_PERCENT%)")
            return false
        }

        val cooldown = ocr.state.value.fresh(TimerRegion.COOLDOWN, time)
        if (cooldown == null) {
            println("[SammeUseCase] 방어: TimerRegion.COOLDOWN(쿨타임 박스) 읽기 실패, 미설정 또는 5초 이상 지연됨")
            return false
        }
        
        val remaining = cooldown.find(NAME)?.remainingSeconds(time)
        if (remaining != null && remaining > 0) {
            println("[SammeUseCase] 방어: 쿨타임 박스에 $NAME $remaining 초 남음으로 인식됨")
            return false
        }
        
        println("[SammeUseCase] 조건 모두 통과! 삼매진화 시전 준비 완료")
        return true
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
        /** 체력이 이 이상이면 쓴다 */
        const val MIN_HP_PERCENT = 50
        const val RECAST_GUARD_MILLIS = 5_000L
    }
}
