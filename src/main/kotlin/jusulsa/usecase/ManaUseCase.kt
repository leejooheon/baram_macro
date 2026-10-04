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
        val vitals = ocr.state.value.freshVitals(time)
        val mp = vitals?.mpPercent
        if (mp == null) {
            log(if (vitals == null) "체력/마력 막대 읽기 실패 또는 5초 이상 지연됨" else "마력 막대를 못 찾음")
            return false
        }
        if (mp > OcrStateHolder.MANA_LOW_PERCENT) return false

        // 공증 직후에는 막대가 아직 안 바뀌었을 수 있다
        lastGongjeungAt?.let {
            if (time - it < RECAST_GUARD_MILLIS) {
                log("마력 $mp% 이지만 ${time - it}ms 전에 공증해서 대기")
                return false
            }
        }

        // 쿨타임 박스에 공력증강이 보이면 아직 못 쓴다
        val cooldown = ocr.state.value.fresh(TimerRegion.COOLDOWN, time)
        val remaining = cooldown?.find(GONGJEUNG_NAME)?.remainingSeconds(time)
        if (remaining != null && remaining > 0) {
            log("마력 $mp% 이지만 쿨타임 박스에 $GONGJEUNG_NAME $remaining 초 남음")
            return false
        }
        log("마력 $mp% -> 공증 (쿨타임 박스 ${if (cooldown == null) "못 읽음" else "통과"})")
        return true
    }

    /** 마력이 부족하면 공증한다. 했으면 true */
    suspend operator fun invoke(): Boolean {
        if (!needsGongjeung()) return false
        val mp = ocr.state.value.freshVitals(now())?.mpPercent
        gongjeung(empty = mp != null && mp <= EMPTY_MP_PERCENT)
        return true
    }

    private fun log(message: String) = println("[ManaUseCase] $message")

    private suspend fun gongjeung(empty: Boolean) {
        Keyboard.atomic {
            if (empty) {
                repeat(2) { Keyboard.pressAndRelease(KeyEvent.VK_U) }
            }
            SkillCaster.cast(Skill.GONGJEUNG)
        }
        lastGongjeungAt = now()
    }

    companion object {
        const val GONGJEUNG_NAME = "공력증강"
        /**
         * 이 이하면 마력 0으로 보고 U를 두 번 누른다. 삼매 뒤 막대가 바로 0이 되지 않고
         * 몇 % 남은 것처럼 읽히는 동안에는 실제 마력이 0이라 U 없이 공증하면 실패한다
         */
        const val EMPTY_MP_PERCENT = 5
        const val RECAST_GUARD_MILLIS = 3_000L
    }
}
