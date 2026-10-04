package detector

import common.network.OcrClient
import common.network.createHttpClient
import common.util.NetworkError
import common.util.Result
import detector.model.DetectedObjectModel
import io.ktor.client.plugins.logging.LogLevel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ocr.TimerMonitor
import ocr.capture.GameWindowCapture
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

/**
 * 게임 창 전체를 주기적으로 찍어 OCR 서버 `/detect/`로 몹과 내 캐릭터 위치를 읽고 [DetectionStateHolder]에 넣는다.
 * 화면 확인용으로 박스를 그린 작은 미리보기도 만든다.
 */
object DetectionMonitor {
    data class State(
        val isRunning: Boolean = false,
        val intervalMillis: Long = 300,
        val detection: Detection? = null,
        /** 박스를 그린 축소 화면. 미리보기를 켰을 때만 만든다 */
        val preview: BufferedImage? = null,
        val showPreview: Boolean = false,
        val error: String? = null,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OcrClient(createHttpClient(logLevel = LogLevel.NONE))
    private var job: Job? = null
    private var window: GameWindowCapture.GameWindow? = null

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun start() {
        if (job?.isActive == true) return
        _state.update { it.copy(isRunning = true, error = null) }
        job = scope.launch {
            while (isActive) {
                val startedAt = System.currentTimeMillis()
                tick()
                val elapsed = System.currentTimeMillis() - startedAt
                delay((state.value.intervalMillis - elapsed).coerceAtLeast(30))
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.update { it.copy(isRunning = false) }
    }

    fun setShowPreview(show: Boolean) {
        _state.update { it.copy(showPreview = show, preview = if (show) it.preview else null) }
    }

    private suspend fun tick() {
        val target = window?.takeIf { GameWindowCapture.isAlive(it) }
            ?: GameWindowCapture.find(TimerMonitor.state.value.windowKeyword)
        window = target
        if (target == null) {
            _state.update { it.copy(error = "게임 창 없음") }
            return
        }
        val capturedAt = System.currentTimeMillis()
        val frame = runCatching { GameWindowCapture.capture(target) }.getOrNull()
        if (frame == null) {
            _state.update { it.copy(error = "창 캡처 실패") }
            return
        }

        when (val result = client.detect(frame)) {
            is Result.Success -> {
                val objects = result.data.objects
                // 내 캐릭터가 여러 개로 잡히면 가장 확실한 것 하나만
                val me = objects.filter { it.label == ME }.maxByOrNull { it.confidence }?.rectangle()
                val detection = Detection(
                    window = target.hwnd,
                    windowWidth = frame.width,
                    windowHeight = frame.height,
                    me = me,
                    monsters = objects.filter { it.label == MONSTER }.map { it.rectangle() }.withoutMe(me),
                    capturedAt = capturedAt,
                    latencyMillis = System.currentTimeMillis() - capturedAt,
                )
                DetectionStateHolder.update(detection)
                _state.update {
                    it.copy(
                        detection = detection,
                        preview = if (it.showPreview) preview(frame, detection) else null,
                        error = null,
                    )
                }
            }
            is Result.Error -> _state.update {
                it.copy(
                    error = when (result.error) {
                        NetworkError.REQUEST_TIMEOUT -> "OCR 서버에 연결할 수 없어요"
                        NetworkError.SERVER_ERROR -> "서버에 탐지 모델이 없어요 (학습 필요)"
                        else -> "탐지 실패 (${result.error.name})"
                    }
                )
            }
        }
    }

    /** 몹은 빨강, 나는 초록, 가장 가까운 몹은 굵게 그린 축소 화면 */
    private fun preview(frame: BufferedImage, detection: Detection): BufferedImage {
        val scale = PREVIEW_WIDTH.toDouble() / frame.width
        val image = BufferedImage(PREVIEW_WIDTH, (frame.height * scale).roundToInt(), BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.drawImage(frame, 0, 0, image.width, image.height, null)
            fun draw(box: Rectangle, color: Color, width: Float) {
                g.color = color
                g.stroke = BasicStroke(width)
                g.drawRect((box.x * scale).toInt(), (box.y * scale).toInt(), (box.width * scale).toInt(), (box.height * scale).toInt())
            }
            val nearest = detection.nearestMonster()
            detection.monsters.forEach { draw(it, Color.RED, if (it == nearest) 3f else 1f) }
            detection.me?.let { draw(it, Color.GREEN, 2f) }
            if (nearest != null) {
                val me = detection.myPosition
                val target = nearest.center()
                g.color = Color.YELLOW
                g.stroke = BasicStroke(1f)
                g.drawLine((me.x * scale).toInt(), (me.y * scale).toInt(), (target.x * scale).toInt(), (target.y * scale).toInt())
            }
        } finally {
            g.dispose()
        }
        return image
    }

    private fun DetectedObjectModel.rectangle() = box.let { (x, y, w, h) -> Rectangle(x, y, w, h) }

    const val MONSTER = "monster"
    const val ME = "me"
    private const val PREVIEW_WIDTH = 480
}
