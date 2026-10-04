package jusulsa.usecase

import jusulsa.skill.SkillInput
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
) {
    enum class Buff(val label: String, val key: Int, val forMe: Boolean) {
        BOHO("보호", jusulsa.skill.BOHO, forMe = true),
        MUJANG("무장", jusulsa.skill.MUJANG, forMe = false),
    }

    private val lastCastAt = ConcurrentHashMap<Buff, Long>()

    /** 지금 다시 걸어야 하는 버프 */
    fun buffsToRenew(): List<Buff> {
        val time = now()
        val panel = ocr.state.value.fresh(TimerRegion.BUFF, time)
        return Buff.entries.filter { needsRenew(it, panel, time) }
    }

    /** 걸어야 하는 보호·무장을 건다. 하나라도 걸었으면 true */
    suspend operator fun invoke(): Boolean {
        val buffs = buffsToRenew()
        buffs.forEach { buff ->
            SkillInput.castAlphabetMagic(buff.key, forMe = buff.forMe, enter = true)
            lastCastAt[buff] = now()
        }
        return buffs.isNotEmpty()
    }

    private fun needsRenew(buff: Buff, panel: RegionResult?, time: Long): Boolean {
        val last = lastCastAt[buff]
        // 건 직후에는 OCR에 아직 안 잡히므로 잠깐 기다린다
        if (last != null && time - last < RECAST_GUARD_MILLIS) return false

        val byTimer = last == null || time - last >= FALLBACK_INTERVAL_MILLIS
        // 패널을 못 읽었거나 버프가 하나도 안 보이면 패널이 가려졌을 수도 있으니 타이머로 판단한다
        if (panel == null || panel.entries.isEmpty()) return byTimer

        val entry = panel.find(buff.label) ?: return true
        val remaining = entry.remainingSeconds(time) ?: return byTimer
        return remaining <= RENEW_BEFORE_SECONDS
    }

    companion object {
        /** 남은 시간이 이 이하가 되면 다시 건다 */
        const val RENEW_BEFORE_SECONDS = 10
        const val RECAST_GUARD_MILLIS = 5_000L
        const val FALLBACK_INTERVAL_MILLIS = 160_000L
    }
}
