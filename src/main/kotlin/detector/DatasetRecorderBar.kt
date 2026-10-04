package detector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ocr.presentation.Chip
import ocr.presentation.SmallText
import java.awt.Desktop

private val intervals = listOf(1000L, 1500L, 3000L)

/** 몹 탐지 학습용 캡처 수집 줄 */
@Composable
fun DatasetRecorderBar() {
    val state by DatasetRecorder.state.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Divider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            val color = if (state.isRecording) Color(0xFFC62828) else Color.Gray
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (state.isRecording) "학습 캡처 중 +${state.saved}" else "학습 캡처",
                style = SmallText,
                color = color,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "총 ${state.total}장" + (state.error?.let { " · $it" } ?: ""),
                style = SmallText,
                color = if (state.error != null) Color.Red else Color.Gray,
            )
            Spacer(Modifier.weight(1f))
            intervals.forEach { millis ->
                Chip(
                    text = "${millis / 1000.0}s".replace(".0s", "s"),
                    selected = state.intervalMillis == millis,
                    onClick = { DatasetRecorder.setInterval(millis) },
                )
            }
            Spacer(Modifier.width(6.dp))
            Chip(
                text = if (state.isRecording) "■" else "●",
                selected = state.isRecording,
                onClick = { if (state.isRecording) DatasetRecorder.stop() else DatasetRecorder.start() },
            )
            Chip(
                text = "폴더",
                selected = false,
                onClick = {
                    DatasetRecorder.directory.mkdirs()
                    runCatching { Desktop.getDesktop().open(DatasetRecorder.directory) }
                },
            )
        }
    }
}
