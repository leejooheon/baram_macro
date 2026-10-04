package ocr.presentation

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import ocr.TimerMonitor
import ocr.model.TimerRegion
import java.awt.image.BufferedImage

/**
 * OCR 모니터 화면과 영역 지정 창. 단독 OCR 모니터와 주술사 앱이 같이 쓴다.
 * header에는 화면 맨 위, footer에는 맨 아래에 붙일 내용(예: 매크로 상태, 창 투명도)을 넣는다.
 */
@Composable
fun OcrMonitorApp(
    header: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
) {
    val state by TimerMonitor.state.collectAsState()
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf<Pair<TimerRegion, BufferedImage>?>(null) }

    LaunchedEffect(Unit) { TimerMonitor.start() }

    MaterialTheme {
        OcrMonitorScreen(
            state = state,
            header = header,
            footer = footer,
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
