package jusulsa

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import common.event.ObserveKeyEvents
import ocr.presentation.OcrMonitorApp

private val TABS = listOf("모니터", "테스트")

/** 주술사 매크로 단축키 + OCR 모니터 화면 + 기능 테스트 탭 */
@Composable
fun JusulsaApp(
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
) {
    val viewModel = remember { JusulsaViewModel2() }
    val uiState by viewModel.uiState.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    ObserveKeyEvents(
        onReleased = viewModel::dispatchKeyReleaseEvent,
        onPressed = viewModel::dispatchKeyPressEvent,
        consumeKeys = JusulsaViewModel2.MACRO_KEYS,
    )

    MaterialTheme {
        Surface {
            Column {
                TabRow(selectedTabIndex = tab) {
                    TABS.forEachIndexed { index, title ->
                        Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
                    }
                }
                when (tab) {
                    0 -> OcrMonitorApp(
                        header = { JusulsaStatusBar(uiState) },
                        footer = { JusulsaOpacityBar(opacity, onOpacityChange) },
                    )
                    else -> JusulsaTestTab(
                        uiState = uiState,
                        onTest = viewModel::test,
                        onStop = viewModel::stop,
                    )
                }
            }
        }
    }
}
