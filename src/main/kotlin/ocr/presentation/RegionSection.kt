package ocr.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ocr.OcrStateHolder
import ocr.model.TimerMonitorState.RegionState
import ocr.model.TimerMonitorState.TimerEntry
import ocr.model.TimerRegion
import kotlin.math.roundToInt

internal val ParsedColor = Color(0xFF2E7D32)
internal val UnparsedColor = Color(0xFFEF6C00)

private val ThumbnailWidth = 120.dp

/** 영역 하나: 왼쪽은 캡처 썸네일(인식한 줄 표시), 오른쪽은 인식 결과 목록 */
@Composable
internal fun RegionSection(
    region: TimerRegion,
    state: RegionState,
    now: Long,
    onPickRegion: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(region.title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text(
                text = state.error ?: when {
                    state.capturedAt == 0L -> ""
                    !region.usesOcr -> "${state.latencyMillis}ms · 100% = ${state.fullWidth}px"
                    state.cached -> "${state.latencyMillis}ms · 변화 없음"
                    else -> "${state.latencyMillis}ms · OCR ${state.ocrMillis.roundToInt()}ms"
                },
                style = SmallText,
                color = if (state.error != null) Color.Red else Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Chip("영역", selected = false, onClick = onPickRegion)
        }

        Row(verticalAlignment = Alignment.Top) {
            Thumbnail(state)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                if (!region.usesOcr) {
                    VitalRow("체력", state.vitals?.hpPercent, HpColor)
                    VitalRow("마력", state.vitals?.mpPercent, MpColor)
                } else if (state.entries.isEmpty()) {
                    Text(
                        text = if (state.image == null) "캡처 없음" else "없음",
                        style = SmallText,
                        color = Color.Gray,
                    )
                } else {
                    state.entries.forEach { EntryRow(it, now) }
                }
            }
        }
    }
}

/** 캡처 위에 서버가 찾은 줄 위치를 그린다 */
@Composable
private fun Thumbnail(state: RegionState) {
    val image = state.image
    if (image == null) {
        Box(Modifier.width(ThumbnailWidth).height(24.dp).background(Color(0xFFEEEEEE)))
        return
    }
    val bitmap = remember(image) { image.toComposeImageBitmap() }
    Canvas(
        modifier = Modifier
            .width(ThumbnailWidth)
            .aspectRatio(image.width.toFloat() / image.height.coerceAtLeast(1))
            .border(1.dp, Color.LightGray)
    ) {
        val scale = size.width / image.width
        drawImage(
            image = bitmap,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        )
        state.bars?.let { bars ->
            // 찾은 막대가 찬 부분
            listOfNotNull(bars.hpRows to bars.hpWidth, bars.mpRows?.let { it to bars.mpWidth }).forEach { (rows, width) ->
                drawRect(
                    color = ParsedColor,
                    topLeft = Offset(bars.left * scale - 1, rows.first * scale - 1),
                    size = Size(width.coerceAtLeast(0) * scale + 2, rows.count() * scale + 2),
                    style = Stroke(width = 1.5f),
                )
            }
        }
        state.entries.forEach { entry ->
            drawRect(
                color = if (entry.confidence >= 0.5) ParsedColor else UnparsedColor,
                topLeft = Offset(entry.box.x * scale - 1, entry.box.y * scale - 1),
                size = Size(entry.box.width * scale + 2, entry.box.height * scale + 2),
                style = Stroke(width = 1.5f),
            )
        }
    }
}

private val HpColor = Color(0xFFE53935)
private val MpColor = Color(0xFF1E88E5)

/** 체력/마력 한 줄: 이름, 작은 막대, % */
@Composable
private fun VitalRow(label: String, percent: Int?, color: Color) {
    val low = label == "마력" && percent != null && percent <= OcrStateHolder.MANA_LOW_PERCENT
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(18.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
        Box(
            Modifier
                .weight(1f)
                .height(8.dp)
                .background(Color(0xFFEEEEEE))
        ) {
            Box(
                Modifier
                    .fillMaxWidth((percent ?: 0) / 100f)
                    .fillMaxHeight()
                    .background(color)
            )
        }
        Text(
            text = percent?.let { "$it%" } ?: "-",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (low) Color.Red else Color.Unspecified,
            modifier = Modifier.width(44.dp).padding(start = 4.dp),
        )
    }
}

@Composable
private fun EntryRow(entry: TimerEntry, now: Long) {
    val color = if (entry.confidence >= 0.5) ParsedColor else UnparsedColor
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(18.dp)) {
        Text(
            text = entry.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = entry.remainingSeconds(now)?.let { "${it}초" } ?: "-",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color,
        )
        Text(
            text = "${(entry.confidence * 100).roundToInt()}%",
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.width(32.dp).padding(start = 4.dp),
        )
    }
}
