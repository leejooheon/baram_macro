import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ocr.NativeScreenCapture
import ocr.TimerMonitor
import ocr.model.TimerRegion
import ocr.presentation.OcrMonitorEvent
import ocr.presentation.OcrMonitorScreen
import ocr.presentation.RegionPickerWindow
import java.awt.image.BufferedImage

/**
 * 스킬 쿨타임 / 버프 OCR 모니터.
 * 실행: gradlew run -PmainClass=OcrMonitorKt
 */
fun main() = application {
    val state by TimerMonitor.state.collectAsState()
    val windowState = rememberWindowState(size = DpSize(560.dp, 820.dp), position = WindowPosition.PlatformDefault)
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf<Pair<TimerRegion, BufferedImage>?>(null) }

    LaunchedEffect(Unit) { TimerMonitor.start() }

    Window(
        onCloseRequest = ::exitApplication,
        title = "OCR 모니터",
        state = windowState,
    ) {
        MaterialTheme {
            OcrMonitorScreen(
                state = state,
                onEvent = { event ->
                    when (event) {
                        OcrMonitorEvent.Start -> TimerMonitor.start()
                        OcrMonitorEvent.Stop -> TimerMonitor.stop()
                        OcrMonitorEvent.CheckServer -> scope.launch { TimerMonitor.checkServer() }
                        is OcrMonitorEvent.ChangeInterval -> TimerMonitor.setInterval(event.millis)
                        is OcrMonitorEvent.ChangeRectangle -> TimerMonitor.setRectangle(event.region, event.rectangle)
                        is OcrMonitorEvent.PickRegion -> scope.launch {
                            // 모니터 창이 캡처에 찍히지 않게 잠깐 내린다
                            windowState.isMinimized = true
                            delay(400)
                            val screenshot = withContext(Dispatchers.IO) { NativeScreenCapture.captureFullScreen() }
                            picking = event.region to screenshot
                        }
                    }
                },
            )
        }
    }

    picking?.let { (region, screenshot) ->
        fun close() {
            picking = null
            windowState.isMinimized = false
        }
        MaterialTheme {
            RegionPickerWindow(
                region = region,
                screenshot = screenshot,
                current = state.regions.getValue(region).rectangle,
                onConfirm = {
                    TimerMonitor.setRectangle(region, it)
                    close()
                },
                onCancel = ::close,
            )
        }
    }
}
