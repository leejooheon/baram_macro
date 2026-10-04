package detector

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ocr.TimerMonitor
import ocr.capture.GameWindowCapture
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import kotlin.math.abs

/**
 * 몹 탐지 모델 학습용으로 게임 창을 주기적으로 찍어 `~/.baram_macro/dataset/raw`에 PNG로 저장한다.
 * 가만히 서 있을 때처럼 직전 장과 거의 같은 화면은 건너뛴다.
 * 저장된 사진은 저장소에 올리지 않고 PC에서만 라벨링, 학습에 쓴다 (script/detector 참고).
 */
object DatasetRecorder {
    val directory = File(System.getProperty("user.home"), ".baram_macro/dataset/raw")

    data class State(
        val isRecording: Boolean = false,
        val intervalMillis: Long = 1500,
        /** 이번에 녹화를 시작한 뒤 저장한 장 수 */
        val saved: Int = 0,
        /** 폴더 전체에 쌓인 장 수 */
        val total: Int = 0,
        /** 직전 장과 거의 같아서 건너뛴 장 수 */
        val skipped: Int = 0,
        val error: String? = null,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var window: GameWindowCapture.GameWindow? = null
    private var previous: IntArray? = null

    private val _state = MutableStateFlow(State(total = countImages()))
    val state: StateFlow<State> = _state.asStateFlow()

    fun start() {
        if (job?.isActive == true) return
        directory.mkdirs()
        previous = null
        _state.update { it.copy(isRecording = true, saved = 0, skipped = 0, error = null, total = countImages()) }
        job = scope.launch {
            while (isActive) {
                val startedAt = System.currentTimeMillis()
                tick()
                val elapsed = System.currentTimeMillis() - startedAt
                delay((state.value.intervalMillis - elapsed).coerceAtLeast(100))
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.update { it.copy(isRecording = false) }
    }

    fun setInterval(millis: Long) {
        _state.update { it.copy(intervalMillis = millis) }
    }

    private fun tick() {
        val target = window?.takeIf { GameWindowCapture.isAlive(it) }
            ?: GameWindowCapture.find(TimerMonitor.state.value.windowKeyword)
        window = target
        if (target == null) {
            _state.update { it.copy(error = "게임 창 없음") }
            return
        }
        val frame = runCatching { GameWindowCapture.capture(target) }.getOrNull()
        if (frame == null) {
            _state.update { it.copy(error = "창 캡처 실패") }
            return
        }

        val thumbnail = thumbnail(frame)
        if (previous?.let { isSimilar(it, thumbnail) } == true) {
            _state.update { it.copy(skipped = it.skipped + 1, error = null) }
            return
        }
        previous = thumbnail

        val name = LocalDateTime.now().format(FILE_NAME) + ".png"
        val written = runCatching { ImageIO.write(frame, "png", File(directory, name)) }.getOrDefault(false)
        _state.update {
            if (written) it.copy(saved = it.saved + 1, total = it.total + 1, error = null)
            else it.copy(error = "저장 실패")
        }
    }

    /** 화면을 32×18 회색조로 줄인 값. 두 장을 빠르게 비교하는 데만 쓴다 */
    private fun thumbnail(image: BufferedImage): IntArray = IntArray(THUMB_W * THUMB_H) { i ->
        val x = (i % THUMB_W) * image.width / THUMB_W + image.width / THUMB_W / 2
        val y = (i / THUMB_W) * image.height / THUMB_H + image.height / THUMB_H / 2
        val rgb = image.getRGB(x, y)
        ((rgb shr 16 and 0xFF) + (rgb shr 8 and 0xFF) + (rgb and 0xFF)) / 3
    }

    private fun isSimilar(a: IntArray, b: IntArray): Boolean =
        a.indices.sumOf { abs(a[it] - b[it]) }.toDouble() / a.size < SIMILAR_MEAN_DIFF

    private fun countImages(): Int = directory.listFiles { f -> f.extension == "png" }?.size ?: 0

    private const val THUMB_W = 32
    private const val THUMB_H = 18
    /** 작은 그림의 평균 밝기 차이가 이보다 작으면 같은 화면으로 본다 */
    private const val SIMILAR_MEAN_DIFF = 2.0
    private val FILE_NAME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")
}
