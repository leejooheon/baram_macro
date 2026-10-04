package jusulsa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jusulsa.model.JusulsaUiState
import kotlin.math.roundToInt

private val StatusText = TextStyle(fontSize = 12.sp)

/** OCR 모니터 화면 위에 붙는 매크로 상태 줄 */
@Composable
internal fun JusulsaStatusBar(uiState: JusulsaUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (label, color) =
                if (uiState.isRunning) "매크로 실행 중 (ESC 정지)" to Color(0xFF2E7D32)
                else "매크로 대기" to Color.Gray
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(4.dp))
            Text(label, style = StatusText, color = color)
            Spacer(Modifier.weight(1f))
            if (uiState.count > 0) {
                Text("헬파 대기 ${uiState.count}", style = StatusText, color = Color(0xFFC62828))
            }
        }
        Divider()
    }
}

/** OCR 모니터 화면 맨 아래에 붙는 창 투명도 조절 */
@Composable
internal fun JusulsaOpacityBar(
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Divider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("투명도", style = StatusText)
            Spacer(Modifier.width(8.dp))
            Slider(
                value = opacity,
                onValueChange = onOpacityChange,
                valueRange = 0.2f..1f,
                modifier = Modifier.weight(1f).height(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("${(opacity * 100).roundToInt()}%", style = StatusText)
        }
    }
}
