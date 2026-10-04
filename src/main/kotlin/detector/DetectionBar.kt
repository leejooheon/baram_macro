package detector

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import ocr.presentation.Chip
import ocr.presentation.SmallText

/** 몹/내 캐릭터 탐지 상태 줄. 미리보기를 켜면 박스를 그린 게임 화면을 보여준다 */
@Composable
fun DetectionBar() {
    val state by DetectionMonitor.state.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Divider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            val color = when {
                !state.isRunning -> Color.Gray
                state.error != null -> Color.Red
                else -> Color(0xFF2E7D32)
            }
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(4.dp))
            Text(text = "몹 탐지", style = SmallText, color = color)
            Spacer(Modifier.width(6.dp))
            Text(
                text = state.error ?: state.detection?.summary() ?: "",
                style = SmallText,
                color = if (state.error != null) Color.Red else Color.Gray,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Chip(text = "미리보기", selected = state.showPreview, onClick = { DetectionMonitor.setShowPreview(!state.showPreview) })
            Chip(
                text = if (state.isRunning) "■" else "▶",
                selected = state.isRunning,
                onClick = { if (state.isRunning) DetectionMonitor.stop() else DetectionMonitor.start() },
            )
        }
        val preview = state.preview
        if (state.showPreview && preview != null) {
            val bitmap = remember(preview) { preview.toComposeImageBitmap() }
            Image(bitmap = bitmap, contentDescription = "탐지 미리보기", modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun Detection.summary(): String {
    val me = me?.center()?.let { "나 (${it.x},${it.y})" } ?: "나 못 찾음"
    val nearest = nearestMonster()?.center()?.let { " · 가까운 몹 (${it.x},${it.y})" } ?: ""
    return "$me · 몹 ${monsters.size}마리$nearest · ${latencyMillis}ms"
}
