package jusulsa.usecase

import common.robot.Keyboard
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import ocr.OcrStateHolder
import ocr.model.TimerRegion
import java.awt.event.KeyEvent

/**
 * 마력이 떨어졌을 때만 공력증강. 공증으로 깎인 체력은 [HealUseCase]가 채운다.
 * 마력이 0이면 공증을 쓸 수 없으니 먼저 U를 두 번 눌러 쓸 수 있는 상태로 만든다.
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
        if (mp > MP_THRESHOLD_PERCENT) return false

        // 쿨타임 박스에 공력증강이 보이면 아직 못 쓴다
        val cooldown = ocr.state.value.fresh(TimerRegion.COOLDOWN, time)
        val remaining = cooldown?.find(GONGJEUNG_NAME)?.remainingSeconds(time)
        return remaining == null || remaining <= 0
    }

    /** 마력이 부족하면 공증한다. 했으면 true */
    suspend operator fun invoke(): Boolean {
        if (!needsGongjeung()) return false

        val mp = ocr.state.value.freshVitals(now())?.mpPercent
        Keyboard.atomic {
            if (mp != null && mp <= 0) {
                Keyboard.pressAndRelease(KeyEvent.VK_U)
                kotlinx.coroutines.delay(100) // 아이템 창 뜨거나 반응할 여유
                Keyboard.pressAndRelease(KeyEvent.VK_U)
                kotlinx.coroutines.delay(200) // 마력 찼음을 서버가 인지할 여유
            }
            SkillCaster.tryCast(Skill.GONGJEUNG) // 원본처럼 블로킹 cast 말고 tryCast로 시도
        }
        lastGongjeungAt = now()
        return true
    }

    companion object {
        const val GONGJEUNG_NAME = "공력증강"
        const val RECAST_GUARD_MILLIS = 3_000L
        const val MP_THRESHOLD_PERCENT = 30 // 마력이 30% 이하일 때 공증 시도
    }
}
