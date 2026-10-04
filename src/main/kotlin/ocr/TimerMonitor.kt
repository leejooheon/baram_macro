package ocr

import common.network.OcrClient
import common.network.createHttpClient
import common.util.NetworkError
import common.util.Result
import io.ktor.client.plugins.logging.LogLevel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ocr.model.TimerMonitorState
import ocr.model.TimerMonitorState.ServerState
import ocr.model.TimerMonitorState.TimerEntry
import ocr.model.TimerRegion
import java.awt.Rectangle

/**
 * 스킬 쿨타임 박스와 버프 패널을 주기적으로 캡처해서 OCR 서버로 읽는다.
 * 다른 매크로에서는 [state]의 remaining(...)으로 남은 초를 읽어 쓰면 된다.
 *
 * 남은 초는 읽은 시점 기준으로 계속 줄어들게 계산하므로, OCR 주기를 길게 잡아도 표시는 매초 갱신된다.
 */
object TimerMonitor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // 매 요청마다 PNG 본문을 로그로 찍지 않도록 로그를 끈 클라이언트를 따로 쓴다
    private val client = OcrClient(createHttpClient(logLevel = LogLevel.NONE))
    private var job: Job? = null

    private val _state = MutableStateFlow(
        TimerMonitorState.default.copy(
            regions = RegionStore.load().mapValues { (_, rect) -> TimerMonitorState.RegionState(rectangle = rect) }
        )
    )
    val state: StateFlow<TimerMonitorState> = _state.asStateFlow()

    fun start() {
        if (job?.isActive == true) return
        _state.update { it.copy(isRunning = true) }
        job = scope.launch {
            checkServer()
            while (isActive) {
                val startedAt = System.currentTimeMillis()
                TimerRegion.entries.forEach { read(it) }
                val elapsed = System.currentTimeMillis() - startedAt
                delay((state.value.intervalMillis - elapsed).coerceAtLeast(50))
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.update { it.copy(isRunning = false) }
    }

    fun setInterval(millis: Long) {
        _state.update { it.copy(intervalMillis = millis) }
    }

    fun setRectangle(region: TimerRegion, rectangle: Rectangle) {
        _state.update { state ->
            val regionState = state.regions.getValue(region)
            state.copy(regions = state.regions + (region to regionState.copy(rectangle = rectangle)))
        }
        RegionStore.save(state.value.regions.mapValues { it.value.rectangle })
        // 바뀐 영역을 바로 한 번 읽어서 보여준다
        scope.launch { read(region) }
    }

    suspend fun checkServer() {
        val server = when (val result = client.health()) {
            is Result.Success -> ServerState.Connected(gpu = result.data.gpu, threads = result.data.threads)
            is Result.Error -> ServerState.Disconnected
        }
        _state.update { it.copy(server = server) }
    }

    private suspend fun read(region: TimerRegion) {
        val rectangle = state.value.regions.getValue(region).rectangle
        val capturedAt = System.currentTimeMillis()
        val image = runCatching { NativeScreenCapture.capture(rectangle) }.getOrNull() ?: return

        val result = client.readTimers(image)
        val latency = System.currentTimeMillis() - capturedAt

        _state.update { state ->
            val previous = state.regions.getValue(region)
            val next = when (result) {
                is Result.Success -> previous.copy(
                    image = image,
                    entries = result.data.lines.map { line ->
                        TimerEntry(
                            name = line.name,
                            raw = line.raw,
                            seconds = line.seconds,
                            confidence = line.confidence,
                            box = line.box.let { (x, y, w, h) -> Rectangle(x, y, w, h) },
                            capturedAt = capturedAt,
                        )
                    },
                    latencyMillis = latency,
                    ocrMillis = result.data.elapsedMs,
                    cached = result.data.cached,
                    capturedAt = capturedAt,
                    error = null,
                )
                is Result.Error -> previous.copy(
                    image = image,
                    capturedAt = capturedAt,
                    error = result.error.toMessage(),
                )
            }
            state.copy(
                regions = state.regions + (region to next),
                server = when {
                    result is Result.Error && result.error == NetworkError.REQUEST_TIMEOUT -> ServerState.Disconnected
                    state.server is ServerState.Disconnected -> ServerState.Unknown
                    else -> state.server
                }
            )
        }
        if (state.value.server is ServerState.Unknown) checkServer()
    }

    private fun NetworkError.toMessage() = when (this) {
        NetworkError.REQUEST_TIMEOUT -> "OCR 서버에 연결할 수 없어요"
        NetworkError.SERVER_ERROR -> "OCR 서버 오류"
        else -> "OCR 실패 ($name)"
    }
}
