package jusulsa.usecase

import jusulsa.engine.MacroUseCase
import jusulsa.engine.ReasonLog
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.Target
import ocr.OcrStateHolder
import ocr.RegionResult
import ocr.model.TimerRegion
import java.util.concurrent.ConcurrentHashMap

/**
 * 보호·무장 갱신. 쿨타임이 없으니 버프 패널의 남은 시간만 보고 끊기기 직전에 다시 건다.
 * OCR 결과가 없으면(서버 꺼짐, 창 못 찾음 등) 예전처럼 마지막으로 건 뒤 160초마다 건다.
 */
class BomuUseCase(
    private val ocr: OcrStateHolder = OcrStateHolder,
    private val now: () -> Long = System::currentTimeMillis,
) : MacroUseCase {
    override val name = "보무"
    private val reason = ReasonLog("BomuUseCase")

    enum class Buff(val label: String, val skill: Skill, val target: Target) {
        BOHO("보호", Skill.BOHO, Target.Me),
        MUJANG("무장", Skill.MUJANG, Target.Confirm),
    }

    private val lastCastAt = ConcurrentHashMap<Buff, Long>()

    /** 지금 다시 걸어야 하는 버프 */
    fun buffsToRenew(): List<Buff> {
        val time = now()
        val panel = ocr.state.value.fresh(TimerRegion.BUFF, time)
        return Buff.entries.filter { needsRenew(it, panel, time) }
    }

    override fun isReady(now: Long) = buffsToRenew().isNotEmpty()

    /** 걸어야 하는 것 중 하나만 건다. 나머지는 엔진이 다음에 다시 고른다 */
    override suspend fun execute() {
        val buff = buffsToRenew().firstOrNull() ?: return
        if (SkillCaster.tryCast(buff.skill, buff.target)) lastCastAt[buff] = now()
    }

    private fun needsRenew(buff: Buff, panel: RegionResult?, time: Long): Boolean {
        val last = lastCastAt[buff]
        // 건 직후에는 OCR에 아직 안 잡히므로 잠깐 기다린다
        if (last != null && time - last < RECAST_GUARD_MILLIS) return false

        val byTimer = last == null || time - last >= FALLBACK_INTERVAL_MILLIS
        // 패널을 못 읽었거나 버프가 하나도 안 보이면 패널이 가려졌을 수도 있으니 타이머로 판단한다
        if (panel == null || panel.entries.isEmpty()) {
            if (byTimer) reason.log("${buff.label}: 버프 패널 못 읽음 -> 시간 기준으로 다시 걸기")
            return byTimer
        }

        val entry = panel.find(buff.label)
        if (entry == null) {
            // 건 지 얼마 안 됐는데 안 보이면 끝난 게 아니라 이름을 못 읽은 것이다. 5초마다 다시 걸지 않게 한다
            if (last != null && time - last < MISSING_GRACE_MILLIS) {
                reason.log("${buff.label}: 패널에 안 보이지만 ${(time - last) / 1000}초 전에 걸어서 대기 (읽은 이름: ${panel.entries.joinToString { it.name }})")
                return false
            }
            reason.log("${buff.label}: 패널에 없음 -> 다시 걸기 (읽은 이름: ${panel.entries.joinToString { it.name }})")
            return true
        }
        val remaining = entry.remainingSeconds(time) ?: return byTimer
        if (remaining <= RENEW_BEFORE_SECONDS) reason.log("${buff.label}: $remaining 초 남음 -> 다시 걸기")
        return remaining <= RENEW_BEFORE_SECONDS
    }

    companion object {
        /** 남은 시간이 이 이하가 되면 다시 건다 */
        const val RENEW_BEFORE_SECONDS = 10
        const val RECAST_GUARD_MILLIS = 5_000L
        const val FALLBACK_INTERVAL_MILLIS = 160_000L
        /** 건 뒤 이 시간 안에는 패널에 이름이 안 보여도 다시 걸지 않는다 */
        const val MISSING_GRACE_MILLIS = 60_000L
    }
}
