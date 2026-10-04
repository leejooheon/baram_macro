package ocr.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ocr.model.TimerMonitorState
import ocr.model.TimerMonitorState.ServerState
import ocr.model.TimerRegion
import java.awt.Rectangle

sealed interface OcrMonitorEvent {
    data object Start : OcrMonitorEvent
    data object Stop : OcrMonitorEvent
    data object CheckServer : OcrMonitorEvent
    data class ChangeInterval(val millis: Long) : OcrMonitorEvent
    data class PickRegion(val region: TimerRegion) : OcrMonitorEvent
    data class ChangeRectangle(val region: TimerRegion, val rectangle: Rectangle) : OcrMonitorEvent
}

private val intervals = listOf(500L, 1000L, 2000L, 3000L)

@Composable
fun OcrMonitorScreen(
    state: TimerMonitorState,
    onEvent: (OcrMonitorEvent) -> Unit,
) {
    // 남은 초는 OCR 사이에도 줄어들어야 하므로 화면만 따로 갱신한다
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(250)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ControlBar(state, onEvent)
        TimerRegion.entries.forEach { region ->
            RegionCard(
                region = region,
                state = state.regions.getValue(region),
                now = now,
                onPickRegion = { onEvent(OcrMonitorEvent.PickRegion(region)) },
                onRectangleChanged = { onEvent(OcrMonitorEvent.ChangeRectangle(region, it)) },
            )
        }
    }
}

@Composable
private fun ControlBar(
    state: TimerMonitorState,
    onEvent: (OcrMonitorEvent) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (label, color) = when (val server = state.server) {
                is ServerState.Connected ->
                    "OCR 서버 연결됨 · ${if (server.gpu) "GPU" else "CPU ${server.threads}스레드"}" to ParsedColor
                ServerState.Disconnected -> "OCR 서버 연결 안 됨 (script/run_ocr_server.bat)" to Color.Red
                ServerState.Unknown -> "OCR 서버 확인 중" to Color.Gray
            }
            Text(label, color = color, style = MaterialTheme.typography.subtitle2)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onEvent(OcrMonitorEvent.CheckServer) }) { Text("다시 확인") }
            if (state.isRunning) {
                OutlinedButton(onClick = { onEvent(OcrMonitorEvent.Stop) }) { Text("멈춤") }
            } else {
                Button(onClick = { onEvent(OcrMonitorEvent.Start) }) { Text("인식 시작") }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("읽는 주기", style = MaterialTheme.typography.body2)
            Spacer(Modifier.width(8.dp))
            intervals.forEach { millis ->
                val selected = state.intervalMillis == millis
                TextButton(
                    onClick = { onEvent(OcrMonitorEvent.ChangeInterval(millis)) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (selected) MaterialTheme.colors.primary else Color.Gray
                    ),
                ) {
                    Text(if (millis < 1000) "${millis / 1000.0}초" else "${millis / 1000}초")
                }
            }
        }
    }
}
