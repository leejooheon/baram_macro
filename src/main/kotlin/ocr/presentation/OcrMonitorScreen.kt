package ocr.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ocr.model.TimerMonitorState
import ocr.model.TimerMonitorState.ServerState
import ocr.model.TimerMonitorState.WindowState
import ocr.model.TimerRegion

sealed interface OcrMonitorEvent {
    data object Start : OcrMonitorEvent
    data object Stop : OcrMonitorEvent
    data object CheckServer : OcrMonitorEvent
    data class ChangeInterval(val millis: Long) : OcrMonitorEvent
    data class PickRegion(val region: TimerRegion) : OcrMonitorEvent
    data class ChangeWindowKeyword(val keyword: String) : OcrMonitorEvent
}

private val intervals = listOf(500L, 1000L, 2000L, 3000L)
internal val SmallText = TextStyle(fontSize = 12.sp)

@Composable
fun OcrMonitorScreen(
    state: TimerMonitorState,
    onEvent: (OcrMonitorEvent) -> Unit,
    header: @Composable () -> Unit = {},
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
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        header()
        ControlBar(state, onEvent)
        TimerRegion.entries.forEach { region ->
            Divider()
            RegionSection(
                region = region,
                state = state.regions.getValue(region),
                now = now,
                onPickRegion = { onEvent(OcrMonitorEvent.PickRegion(region)) },
            )
        }
    }
}

@Composable
private fun ControlBar(
    state: TimerMonitorState,
    onEvent: (OcrMonitorEvent) -> Unit,
) {
    var showSettings by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (server, serverColor) = when (val s = state.server) {
                is ServerState.Connected -> (if (s.gpu) "서버 GPU" else "서버 CPU${s.threads}") to ParsedColor
                ServerState.Disconnected -> "서버 연결 안 됨" to Color.Red
                ServerState.Unknown -> "서버 확인 중" to Color.Gray
            }
            StatusDot(server, serverColor, Modifier.clickable { onEvent(OcrMonitorEvent.CheckServer) })
            Spacer(Modifier.width(8.dp))
            val (window, windowColor) = when (val w = state.window) {
                is WindowState.Found -> "창 ${w.width}×${w.height}" to ParsedColor
                is WindowState.CaptureFailed -> "창 캡처 실패" to UnparsedColor
                WindowState.NotFound -> "게임 창 없음" to Color.Red
                WindowState.Searching -> "창 찾는 중" to Color.Gray
            }
            StatusDot(window, windowColor)
            Spacer(Modifier.weight(1f))
            intervals.forEach { millis ->
                Chip(
                    text = if (millis < 1000) "${millis / 1000.0}s" else "${millis / 1000}s",
                    selected = state.intervalMillis == millis,
                    onClick = { onEvent(OcrMonitorEvent.ChangeInterval(millis)) },
                )
            }
            Spacer(Modifier.width(6.dp))
            Chip(
                text = if (state.isRunning) "■" else "▶",
                selected = state.isRunning,
                onClick = { onEvent(if (state.isRunning) OcrMonitorEvent.Stop else OcrMonitorEvent.Start) },
            )
            Chip(text = "⚙", selected = showSettings, onClick = { showSettings = !showSettings })
        }

        if (showSettings) {
            WindowKeywordRow(state, onEvent)
        }
    }
}

@Composable
private fun WindowKeywordRow(
    state: TimerMonitorState,
    onEvent: (OcrMonitorEvent) -> Unit,
) {
    var keyword by remember(state.windowKeyword) { mutableStateOf(state.windowKeyword) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("게임 창 제목", style = SmallText, color = Color.Gray)
        Spacer(Modifier.width(6.dp))
        BasicTextField(
            value = keyword,
            onValueChange = { keyword = it },
            singleLine = true,
            textStyle = SmallText,
            modifier = Modifier
                .width(120.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
        )
        if (keyword != state.windowKeyword) {
            Spacer(Modifier.width(4.dp))
            Chip("적용", selected = true, onClick = { onEvent(OcrMonitorEvent.ChangeWindowKeyword(keyword)) })
        }
        Spacer(Modifier.width(6.dp))
        val title = (state.window as? WindowState.Found)?.title ?: (state.window as? WindowState.CaptureFailed)?.title
        Text(
            text = title ?: "",
            style = SmallText,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusDot(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(text, style = SmallText, color = color)
    }
}

@Composable
internal fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colors.primary else Color.Gray
    Text(
        text = text,
        style = SmallText,
        color = color,
        modifier = Modifier
            .padding(horizontal = 1.dp)
            .border(1.dp, color.copy(alpha = if (selected) 1f else 0.4f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
