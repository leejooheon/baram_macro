package ocr.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ocr.model.TimerMonitorState.RegionState
import ocr.model.TimerMonitorState.TimerEntry
import ocr.model.TimerRegion
import kotlin.math.roundToInt

internal val ParsedColor = Color(0xFF2E7D32)
internal val UnparsedColor = Color(0xFFEF6C00)

@Composable
internal fun RegionCard(
    region: TimerRegion,
    state: RegionState,
    now: Long,
    onPickRegion: () -> Unit,
) {
    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(region.title, style = MaterialTheme.typography.h6)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = state.pixels?.let { "게임 창 기준 (${it.x}, ${it.y})  ${it.width}×${it.height}" } ?: "",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray,
                )
                Spacer(Modifier.weight(1f))
                Button(onClick = onPickRegion) { Text("영역 지정") }
            }

            Spacer(Modifier.height(8.dp))
            CaptureWithBoxes(state)
            Spacer(Modifier.height(8.dp))

            if (state.entries.isEmpty()) {
                Text(
                    text = if (state.image == null) "아직 캡처하지 않았어요" else "인식된 글자가 없어요",
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray,
                )
            } else {
                state.entries.forEach { EntryRow(it, now) }
            }

            Spacer(Modifier.height(4.dp))
            StatusLine(state, now)
        }
    }
}

/** 캡처 이미지 위에 서버가 찾은 줄 위치를 그린다. 초록 = 이름과 초를 읽음, 주황 = 초를 못 읽음 */
@Composable
private fun CaptureWithBoxes(state: RegionState) {
    val image = state.image ?: run {
        Box(
            Modifier.fillMaxWidth().height(48.dp).background(Color(0xFFEEEEEE)),
            contentAlignment = Alignment.Center
        ) { Text("캡처 없음", color = Color.Gray) }
        return
    }
    val bitmap = remember(image) { image.toComposeImageBitmap() }
    val aspect = image.width.toFloat() / image.height.coerceAtLeast(1)

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = minOf(maxWidth, 480.dp)
        Canvas(
            modifier = Modifier
                .width(width)
                .aspectRatio(aspect)
                .border(1.dp, Color.LightGray)
        ) {
            val scale = size.width / image.width
            drawImage(
                image = bitmap,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                dstOffset = IntOffset.Zero,
            )
            state.entries.forEach { entry ->
                drawRect(
                    color = if (entry.seconds != null) ParsedColor else UnparsedColor,
                    topLeft = Offset(entry.box.x * scale - 2, entry.box.y * scale - 2),
                    size = Size(entry.box.width * scale + 4, entry.box.height * scale + 4),
                    style = Stroke(width = 2f),
                )
            }
        }
    }
}

@Composable
private fun EntryRow(entry: TimerEntry, now: Long) {
    val color = if (entry.seconds != null) ParsedColor else UnparsedColor
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Box(Modifier.size(10.dp).background(color))
        Spacer(Modifier.width(8.dp))
        Text(
            text = entry.name,
            style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(110.dp),
        )
        Text(
            text = entry.remainingSeconds(now)?.let { "${it}초" } ?: "-",
            style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
            color = color,
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = "\"${entry.raw}\"",
            style = MaterialTheme.typography.caption,
            color = Color.Gray,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${(entry.confidence * 100).roundToInt()}%",
            style = MaterialTheme.typography.caption,
            color = if (entry.confidence < 0.5) UnparsedColor else Color.Gray,
        )
    }
}

@Composable
private fun StatusLine(state: RegionState, now: Long) {
    val text = when {
        state.error != null -> state.error
        state.capturedAt == 0L -> ""
        else -> buildString {
            append("${((now - state.capturedAt) / 1000.0).let { "%.1f".format(it) }}초 전 읽음 · ")
            append("응답 ${state.latencyMillis}ms")
            append(if (state.cached) " (화면 변화 없음, 캐시)" else " (OCR ${state.ocrMillis.roundToInt()}ms)")
        }
    }
    Text(
        text = text,
        fontSize = 11.sp,
        color = if (state.error != null) Color.Red else Color.Gray,
    )
}
