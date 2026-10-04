package jusulsa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import common.event.ObserveKeyEvents
import ocr.presentation.OcrMonitorApp

/** 주술사 매크로 단축키 + OCR 모니터 화면 */
@Composable
fun JusulsaApp(
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
) {
    val viewModel = remember { JusulsaViewModel2() }
    val uiState by viewModel.uiState.collectAsState()

    ObserveKeyEvents(
        onReleased = viewModel::dispatchKeyReleaseEvent,
        onPressed = viewModel::dispatchKeyPressEvent,
        consumeKeys = JusulsaViewModel2.MACRO_KEYS,
    )

    OcrMonitorApp(
        header = { JusulsaStatusBar(uiState) },
        footer = { JusulsaOpacityBar(opacity, onOpacityChange) },
    )
}
