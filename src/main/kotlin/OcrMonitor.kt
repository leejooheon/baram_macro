import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf<Pair<TimerRegion, BufferedImage>?>(null) }

    LaunchedEffect(Unit) { TimerMonitor.start() }

    Window(
        onCloseRequest = ::exitApplication,
        title = "OCR 모니터",
        state = rememberWindowState(size = DpSize(600.dp, 860.dp), position = WindowPosition.PlatformDefault),
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
                        is OcrMonitorEvent.ChangeWindowKeyword -> TimerMonitor.setWindowKeyword(event.keyword)
                        is OcrMonitorEvent.PickRegion -> scope.launch {
                            val frame = TimerMonitor.refreshFrame() ?: state.frame ?: return@launch
                            picking = event.region to frame
                        }
                    }
                },
            )
        }
    }

    picking?.let { (region, frame) ->
        MaterialTheme {
            RegionPickerWindow(
                region = region,
                frame = frame,
                regions = state.regions.mapValues { it.value.fraction },
                onConfirm = {
                    TimerMonitor.setRegion(region, it)
                    picking = null
                },
                onCancel = { picking = null },
            )
        }
    }
}
