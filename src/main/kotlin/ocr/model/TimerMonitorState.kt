package ocr.model

import ocr.vitals.VitalsReader
import java.awt.Rectangle
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage

data class TimerMonitorState(
    val isRunning: Boolean,
    val intervalMillis: Long,
    val server: ServerState,
    val windowKeyword: String,
    val window: WindowState,
    /** 마지막으로 찍은 게임 창 전체 (영역 지정 화면에 쓴다) */
    val frame: BufferedImage?,
    val regions: Map<TimerRegion, RegionState>,
) {
    sealed interface ServerState {
        data object Unknown : ServerState
        data class Connected(val gpu: Boolean, val threads: Int) : ServerState
        data object Disconnected : ServerState
    }

    sealed interface WindowState {
        data object Searching : WindowState
        data class Found(val title: String, val width: Int, val height: Int) : WindowState
        data object NotFound : WindowState
        /** 창은 있지만 최소화 등으로 찍을 수 없음 */
        data class CaptureFailed(val title: String) : WindowState
    }

    data class RegionState(
        /** 게임 창 대비 비율 */
        val fraction: Rectangle2D.Double,
        /** 마지막으로 OCR에 보낸 이미지 */
        val image: BufferedImage? = null,
        val entries: List<TimerEntry> = emptyList(),
        /** 캡처부터 응답까지 걸린 시간 */
        val latencyMillis: Long = 0,
        /** 서버에서 OCR에 쓴 시간 */
        val ocrMillis: Double = 0.0,
        val cached: Boolean = false,
        val capturedAt: Long = 0,
        val error: String? = null,
        /** 체력/마력 영역만: 계산한 % */
        val vitals: Vitals? = null,
        /** 체력/마력 영역만: 찾은 막대 위치 (썸네일에 그린다) */
        val bars: VitalsReader.BarReading? = null,
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
        /** 영역 이미지 픽셀 기준 */
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
}
