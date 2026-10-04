package ocr.model

import java.awt.Rectangle
import java.awt.image.BufferedImage

data class TimerMonitorState(
    val isRunning: Boolean,
    val intervalMillis: Long,
    val server: ServerState,
    val regions: Map<TimerRegion, RegionState>,
) {
    sealed interface ServerState {
        data object Unknown : ServerState
        data class Connected(val gpu: Boolean, val threads: Int) : ServerState
        data object Disconnected : ServerState
    }

    data class RegionState(
        val rectangle: Rectangle,
        /** 마지막으로 OCR에 보낸 캡처 (실제 화면 픽셀 해상도) */
        val image: BufferedImage? = null,
        val entries: List<TimerEntry> = emptyList(),
        /** 캡처부터 응답까지 걸린 시간 */
        val latencyMillis: Long = 0,
        /** 서버에서 OCR에 쓴 시간 */
        val ocrMillis: Double = 0.0,
        val cached: Boolean = false,
        val capturedAt: Long = 0,
        val error: String? = null,
    )

    /**
     * 인식된 한 줄.
     * 읽은 시점(capturedAt)과 초를 같이 들고 있어서, 다음 OCR 전까지는 시간을 빼서 남은 초를 계산한다.
     */
    data class TimerEntry(
        val name: String,
        val raw: String,
        val seconds: Int?,
        val confidence: Double,
        /** 캡처 이미지 픽셀 기준 */
        val box: Rectangle,
        val capturedAt: Long,
    ) {
        fun remainingSeconds(now: Long = System.currentTimeMillis()): Int? =
            seconds?.let { (it - (now - capturedAt) / 1000).toInt().coerceAtLeast(0) }
    }

    fun entries(region: TimerRegion): List<TimerEntry> = regions[region]?.entries.orEmpty()

    /** 버프/쿨타임 이름으로 남은 초. 없으면 null */
    fun remaining(region: TimerRegion, name: String): Int? =
        entries(region).firstOrNull { it.name == name }?.remainingSeconds()

    companion object {
        val default = TimerMonitorState(
            isRunning = false,
            intervalMillis = 1000,
            server = ServerState.Unknown,
            regions = TimerRegion.entries.associateWith { RegionState(rectangle = it.defaultRectangle) },
        )
    }
}
