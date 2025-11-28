package jusulsa

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import common.event.ObserveKeyEvents
import common.event.ObserveMouseEvents

@Composable
fun JusulsaApp() {
    val viewModel = remember { JusulsaViewModel2() }
    val uiState by viewModel.uiState.collectAsState()

    ObserveKeyEvents(
        onReleased = viewModel::dispatchKeyReleaseEvent,
        onPressed = viewModel::dispatchKeyPressEvent
    )

    MaterialTheme {
        JusulsaScreen(
            uiState = uiState
        )
    }
}