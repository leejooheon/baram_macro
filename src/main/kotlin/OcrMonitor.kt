import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ocr.presentation.OcrMonitorApp

/**
 * 스킬 쿨타임 / 버프 OCR 모니터 (매크로 없이 OCR만).
 * 실행: gradlew run -PmainClass=OcrMonitorKt
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "OCR 모니터",
        state = rememberWindowState(size = DpSize(400.dp, 420.dp), position = WindowPosition.PlatformDefault),
    ) {
        OcrMonitorApp()
    }
}
