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
import ocr.capture.GameWindowCapture
import ocr.character.AdjacentMonsterDetector
import ocr.character.CharacterLocator
import ocr.character.CoordinateReader
import ocr.model.TimerMonitorState
import ocr.model.TimerMonitorState.RegionState
import ocr.model.TimerMonitorState.ServerState
import ocr.model.TimerMonitorState.TimerEntry
import ocr.model.TimerMonitorState.WindowState
import ocr.model.TimerLineModel
import ocr.model.TimerRegion
import ocr.model.Vitals
import ocr.vitals.VitalsReader
import java.awt.Rectangle
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

/**
 * 게임 창을 주기적으로 한 장 찍고, 그 안의 쿨타임 박스와 버프 패널을 잘라 OCR 서버로 읽는다.
 * 체력/마력 막대, 내 캐릭터 위치/주변 몬스터/좌표는 서버로 보내지 않고 여기서 읽는다 ([CharacterStateHolder]).
 * 다른 매크로에서는 [state]의 remaining(...)으로 남은 초를 읽어 쓰면 된다.
 *
 * 남은 초는 읽은 시점 기준으로 계속 줄어들게 계산하므로, OCR 주기를 길게 잡아도 표시는 매초 갱신된다.
 */
object TimerMonitor {
    /** 맵 한 칸 크기 / 게임 창 폭. 클라이언트 영역 2554px 폭 창에서 한 칸이 72px였다 */
    private const val TILE_PER_WINDOW_WIDTH = 72.0 / 2554

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // 매 요청마다 PNG 본문을 로그로 찍지 않도록 로그를 끈 클라이언트를 따로 쓴다
    private val client = OcrClient(createHttpClient(logLevel = LogLevel.NONE))
    private var job: Job? = null
    private var window: GameWindowCapture.GameWindow? = null

    private val _state = MutableStateFlow(
        RegionStore.load().let { saved ->
            TimerMonitorState(
                isRunning = false,
                intervalMillis = 1000,
                server = ServerState.Unknown,
                windowKeyword = saved.windowKeyword,
                window = WindowState.Searching,
                frame = null,
                regions = saved.regions.mapValues { (_, fraction) -> RegionState(fraction = fraction) },
            )
        }
    )
    val state: StateFlow<TimerMonitorState> = _state.asStateFlow()

    fun start() {
        if (job?.isActive == true) return
        _state.update { it.copy(isRunning = true) }
        job = scope.launch {
            checkServer()
            while (isActive) {
                val startedAt = System.currentTimeMillis()
                tick()
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

    fun setWindowKeyword(keyword: String) {
        window = null
        _state.update { it.copy(windowKeyword = keyword, window = WindowState.Searching) }
        save()
    }

    fun setRegion(region: TimerRegion, fraction: Rectangle2D.Double) {
        _state.update { state ->
            val regionState = state.regions.getValue(region)
            state.copy(regions = state.regions + (region to regionState.copy(fraction = fraction)))
        }
        save()
        // 바뀐 영역을 바로 한 번 읽어서 보여준다
        scope.launch { tick() }
    }

    /** 영역 지정 화면을 열기 전에 최신 게임 화면을 찍어 둔다 */
    suspend fun refreshFrame(): BufferedImage? = withContext(Dispatchers.IO) { captureFrame() }

    suspend fun checkServer() {
        val server = when (val result = client.health()) {
            is Result.Success -> ServerState.Connected(gpu = result.data.gpu, threads = result.data.threads)
            is Result.Error -> ServerState.Disconnected
        }
        _state.update { it.copy(server = server) }
    }

    private suspend fun tick() {
        val capturedAt = System.currentTimeMillis()
        val target = findWindow() ?: return
        val regions = TimerRegion.entries
        val fractions = regions.map { state.value.regions.getValue(it).fraction }
        // 게임 창 전체가 아니라 영역들만 옮겨 온다
        val capture = runCatching { GameWindowCapture.captureRegions(target, fractions) }.getOrNull()
        _state.update {
            it.copy(
                window = if (capture == null) WindowState.CaptureFailed(target.title)
                         else WindowState.Found(target.title, capture.windowSize.width, capture.windowSize.height)
            )
        }
        capture ?: return
        // 체력/마력과 캐릭터 위치는 서버를 거치지 않으니 먼저 읽는다. OCR 서버가 느리거나 꺼져 있으면
        // 요청마다 수 초씩 걸려서, 뒤에 읽으면 매크로가 쓰기 전에 값이 오래된 것으로 버려진다
        regions.forEachIndexed { i, region ->
            if (region.reader == TimerRegion.Reader.BARS) readVitals(region, capture.images[i], capturedAt)
        }
        readCharacter(
            portrait = capture.images[regions.indexOf(TimerRegion.PORTRAIT)],
            field = capture.images[regions.indexOf(TimerRegion.FIELD)],
            coords = capture.images[regions.indexOf(TimerRegion.COORDS)],
            windowWidth = capture.windowSize.width,
            capturedAt = capturedAt,
        )
        regions.forEachIndexed { i, region ->
            if (region.reader == TimerRegion.Reader.OCR) read(region, capture.images[i], capturedAt)
        }
        if (state.value.server is ServerState.Unknown) checkServer()
    }

    private fun findWindow(): GameWindowCapture.GameWindow? {
        val target = window?.takeIf { GameWindowCapture.isAlive(it) }
            ?: GameWindowCapture.find(state.value.windowKeyword)
        window = target
        if (target == null) _state.update { it.copy(window = WindowState.NotFound) }
        return target
    }

    /** 영역 지정 화면용 게임 창 전체 캡처 */
    private fun captureFrame(): BufferedImage? {
        val target = findWindow() ?: return null
        val frame = runCatching { GameWindowCapture.capture(target) }.getOrNull()
        _state.update {
            it.copy(
                window = if (frame == null) WindowState.CaptureFailed(target.title)
                         else WindowState.Found(target.title, frame.width, frame.height),
                frame = frame ?: it.frame,
            )
        }
        return frame
    }

    private suspend fun read(region: TimerRegion, image: BufferedImage, capturedAt: Long) {
        val result = client.readTimers(image)
        val latency = System.currentTimeMillis() - capturedAt

        OcrStateHolder.update(
            region = region,
            result = when (result) {
                is Result.Success -> RegionResult(
                    entries = result.data.lines.map { it.toEntry(capturedAt) },
                    capturedAt = capturedAt,
                    success = true,
                )
                is Result.Error -> RegionResult(entries = emptyList(), capturedAt = capturedAt, success = false)
            },
        )

        _state.update { state ->
            val previous = state.regions.getValue(region)
            val next = when (result) {
                is Result.Success -> previous.copy(
                    image = image,
                    entries = result.data.lines.map { it.toEntry(capturedAt) },
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
    }

    private fun readVitals(region: TimerRegion, image: BufferedImage, capturedAt: Long) {
        val bars = VitalsReader.read(image)
        val vitals = bars?.let { Vitals(hpPercent = it.hpPercent, mpPercent = it.mpPercent, capturedAt = capturedAt) }
        if (vitals != null) OcrStateHolder.updateVitals(vitals)

        _state.update { state ->
            val next = state.regions.getValue(region).copy(
                image = image,
                capturedAt = capturedAt,
                latencyMillis = System.currentTimeMillis() - capturedAt,
                vitals = vitals,
                bars = bars,
                error = if (bars == null) "막대를 못 찾았어요" else null,
            )
            state.copy(regions = state.regions + (region to next))
        }
    }

    private fun readCharacter(
        portrait: BufferedImage,
        field: BufferedImage,
        coords: BufferedImage,
        windowWidth: Int,
        capturedAt: Long,
    ) {
        val found = CharacterLocator.locate(portrait, field)
        val tileSize = (windowWidth * TILE_PER_WINDOW_WIDTH).roundToInt().coerceAtLeast(8)
        val monsters = found?.let { AdjacentMonsterDetector.detect(field, it, tileSize) }
        val coordinate = CoordinateReader.read(coords)
        CharacterStateHolder.update(
            CharacterState(
                mapX = coordinate?.x,
                mapY = coordinate?.y,
                screenX = found?.let { it.center.x.toDouble() / field.width },
                screenY = found?.let { it.center.y.toDouble() / field.height },
                adjacent = monsters?.occupied.orEmpty(),
                capturedAt = capturedAt,
            )
        )

        val latency = System.currentTimeMillis() - capturedAt
        _state.update { state ->
            val fieldState = state.regions.getValue(TimerRegion.FIELD).copy(
                image = field,
                capturedAt = capturedAt,
                latencyMillis = latency,
                character = found,
                monsters = monsters,
                error = if (found == null) "캐릭터를 못 찾았어요" else null,
            )
            val coordsState = state.regions.getValue(TimerRegion.COORDS).copy(
                image = coords,
                capturedAt = capturedAt,
                latencyMillis = latency,
                coordinate = coordinate,
                error = if (coordinate == null) "좌표를 못 읽었어요" else null,
            )
            state.copy(
                regions = state.regions + (TimerRegion.FIELD to fieldState) + (TimerRegion.COORDS to coordsState)
            )
        }
    }

    private fun TimerLineModel.toEntry(capturedAt: Long) = TimerEntry(
        name = name,
        raw = raw,
        seconds = seconds,
        confidence = confidence,
        box = box.let { (x, y, w, h) -> Rectangle(x, y, w, h) },
        capturedAt = capturedAt,
    )

    private fun save() {
        val state = state.value
        RegionStore.save(state.windowKeyword, state.regions.mapValues { it.value.fraction })
    }

    private fun NetworkError.toMessage() = when (this) {
        NetworkError.REQUEST_TIMEOUT -> "OCR 서버에 연결할 수 없어요"
        NetworkError.SERVER_ERROR -> "OCR 서버 오류"
        else -> "OCR 실패 ($name)"
    }
}
