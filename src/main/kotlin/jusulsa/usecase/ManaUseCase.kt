package jusulsa.usecase

import common.robot.Keyboard
import jusulsa.engine.MacroUseCase
import jusulsa.engine.ReasonLog
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import kotlinx.coroutines.delay
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
) : MacroUseCase {
    override val name = "공증"
    private var lastGongjeungAt: Long? = null
    private val reason = ReasonLog("ManaUseCase")

    override fun isReady(now: Long) = needsGongjeung()

    fun needsGongjeung(): Boolean {
        val time = now()
        val vitals = ocr.state.value.freshVitals(time)
        val mp = vitals?.mpPercent
        if (mp == null) {
            log(if (vitals == null) "체력/마력 막대 읽기 실패 또는 5초 이상 지연됨" else "마력 막대를 못 찾음")
            return false
        }
        if (mp > OcrStateHolder.MANA_LOW_PERCENT) return false

        // 공증 직후에는 막대가 아직 안 바뀌었을 수 있다. 막대를 0.2초마다 읽으니 짧게 두고, 씹혔으면 바로 다시 쓴다
        lastGongjeungAt?.let {
            if (time - it < RECAST_GUARD_MILLIS) {
                log("마력 $mp% 이지만 방금 공증해서 대기")
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

    /** 마력이 0이면 U 두 번 후 공증 */
    override suspend fun execute() {
        val mp = ocr.state.value.freshVitals(now())?.mpPercent
        gongjeung(empty = mp != null && mp <= 0)
    }

    private fun log(message: String) = reason.log(message)

    private suspend fun gongjeung(empty: Boolean) {
        if (empty) {
            // 너무 빨리 누르면 씹혀서 예전 매크로(eat)의 간격을 그대로 쓴다
            repeat(2) {
                delay(U_GAP_MILLIS)
                Keyboard.pressAndRelease(KeyEvent.VK_U, U_PRESS_MILLIS)
            }
        }
        SkillCaster.tryCast(Skill.GONGJEUNG)
        lastGongjeungAt = now()
    }

    companion object {
        /** U를 누르고 있는 시간이자 뗀 뒤 쉬는 시간 */
        const val U_PRESS_MILLIS = 100L
        /** U를 누르기 전에 더 쉬는 시간 */
        const val U_GAP_MILLIS = 60L
        const val GONGJEUNG_NAME = "공력증강"
        const val RECAST_GUARD_MILLIS = 500L
    }
}
