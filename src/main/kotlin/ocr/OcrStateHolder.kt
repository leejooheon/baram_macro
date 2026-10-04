package ocr

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ocr.model.CharacterPosition
import ocr.model.TimerMonitorState.TimerEntry
import ocr.model.TimerRegion
import ocr.model.Vitals

/**
 * 매크로가 쓰는 OCR 결과 저장소. [TimerMonitor]가 영역을 읽을 때마다 여기에 넣고,
 * 각 UseCase는 이 값만 보고 판단한다 (화면 이미지 같은 모니터 UI용 정보는 담지 않는다).
 */
object OcrStateHolder {
    private val _state = MutableStateFlow(OcrResult())
    val state: StateFlow<OcrResult> = _state.asStateFlow()

    /** 값이 바뀌는 게 아니라 어떤 일이 "일어났을 때" 한 번 보내는 이벤트 (예: 마력이 10% 아래로 떨어짐) */
    private val _events = MutableSharedFlow<OcrEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<OcrEvent> = _events.asSharedFlow()

    /** 마력 부족 이벤트를 이미 보냈는지. 다시 [MANA_REARM_PERCENT] 위로 올라가야 다음 이벤트를 보낸다 */
    private var manaLowSent = false

    fun update(region: TimerRegion, result: RegionResult) {
        _state.update { it.copy(regions = it.regions + (region to result)) }
    }

    @Synchronized
    fun updateVitals(vitals: Vitals) {
        _state.update { it.copy(vitals = vitals) }
        val mp = vitals.mpPercent ?: return
        if (!manaLowSent && mp <= MANA_LOW_PERCENT) {
            manaLowSent = true
            _events.tryEmit(OcrEvent.ManaLow(percent = mp, at = vitals.capturedAt))
        } else if (manaLowSent && mp >= MANA_REARM_PERCENT) {
            manaLowSent = false
        }
    }

    /** 내 캐릭터 위치. 못 찾았으면 null로 지운다 */
    fun updateCharacter(position: CharacterPosition?) {
        _state.update { it.copy(character = position) }
    }

    /** 이 값 이하가 되면 [OcrEvent.ManaLow]를 보낸다 */
    const val MANA_LOW_PERCENT = 10
    /** 막대가 경계에서 흔들릴 때 이벤트가 연달아 나가지 않도록, 이만큼 회복해야 다시 보낸다 */
    const val MANA_REARM_PERCENT = 15
}

sealed interface OcrEvent {
    /** 마력이 [OcrStateHolder.MANA_LOW_PERCENT]% 이하로 떨어졌다 */
    data class ManaLow(val percent: Int, val at: Long) : OcrEvent
}

data class OcrResult(
    val regions: Map<TimerRegion, RegionResult> = emptyMap(),
    /** 체력/마력 막대. 아직 못 읽었으면 null */
    val vitals: Vitals? = null,
    /** 맵 화면에서 찾은 내 캐릭터. 못 찾았으면 null */
    val character: CharacterPosition? = null,
) {
    /** 최근에 찾은 내 캐릭터 위치. 오래됐으면 null */
    fun freshCharacter(now: Long = System.currentTimeMillis()): CharacterPosition? =
        character?.takeIf { it.isFresh(now) }

    /** 최근에 읽은 체력/마력. 오래됐으면 null */
    fun freshVitals(now: Long = System.currentTimeMillis()): Vitals? = vitals?.takeIf { it.isFresh(now) }

    /** 최근에 제대로 읽은 영역만 돌려준다. 오래됐거나 읽기에 실패했으면 null */
    fun fresh(region: TimerRegion, now: Long = System.currentTimeMillis()): RegionResult? =
        regions[region]?.takeIf { it.isFresh(now) }
}

data class RegionResult(
    val entries: List<TimerEntry>,
    val capturedAt: Long,
    /** OCR 서버에서 결과를 받았는지 */
    val success: Boolean,
) {
    fun isFresh(now: Long = System.currentTimeMillis()): Boolean =
        success && now - capturedAt <= MAX_AGE_MILLIS

    fun find(name: String): TimerEntry? = entries.firstOrNull { it.name == name }

    companion object {
        /** 이보다 오래된 결과는 믿지 않는다 (OCR 주기 1초 + 응답 지연 여유) */
        const val MAX_AGE_MILLIS = 5_000L
    }
}
