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
    var pickingMonster by remember { mutableStateOf<BufferedImage?>(null) }

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
                    OcrMonitorEvent.AddMonster -> scope.launch {
                        pickingMonster = TimerMonitor.refreshFrame() ?: state.frame ?: return@launch
                    }
                    OcrMonitorEvent.ClearMonsters -> TimerMonitor.clearMonsters()
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

    pickingMonster?.let { frame ->
        MaterialTheme {
            RegionPickerWindow(
                region = TimerRegion.FIELD,
                frame = frame,
                regions = emptyMap(),
                onConfirm = {
                    TimerMonitor.addMonster(frame, it)
                    pickingMonster = null
                },
                onCancel = { pickingMonster = null },
                title = "몬스터 등록",
                hint = "몬스터 한 마리를 테두리에 딱 맞게 드래그하세요. 발밑 바닥이나 다른 몬스터가 들어가지 않게 해 주세요. 앞/뒤/옆 모습을 각각 등록하면 좋아요.",
            )
        }
    }
}
