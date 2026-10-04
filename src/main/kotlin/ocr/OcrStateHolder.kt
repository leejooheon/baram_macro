package ocr

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ocr.model.TimerMonitorState.TimerEntry
import ocr.model.TimerRegion

/**
 * 매크로가 쓰는 OCR 결과 저장소. [TimerMonitor]가 영역을 읽을 때마다 여기에 넣고,
 * 각 UseCase는 이 값만 보고 판단한다 (화면 이미지 같은 모니터 UI용 정보는 담지 않는다).
 */
object OcrStateHolder {
    private val _state = MutableStateFlow(OcrResult())
    val state: StateFlow<OcrResult> = _state.asStateFlow()

    fun update(region: TimerRegion, result: RegionResult) {
        _state.update { it.copy(regions = it.regions + (region to result)) }
    }
}

data class OcrResult(
    val regions: Map<TimerRegion, RegionResult> = emptyMap(),
) {
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
