package commander

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import common.UiStateHolder
import common.event.ObserveKeyEvents
import common.event.ObserveMouseEvents

@Composable
fun CommanderApp() {
    val viewModel = remember { CommanderViewModel() }
    val uiState by UiStateHolder.state.collectAsState()

    ObserveMouseEvents(
        onClicked = {
            println("nativeMouseClicked: $it")
        }
    )

    ObserveKeyEvents(
        onReleased = viewModel::dispatchKeyReleaseEvent,
        onPressed = viewModel::dispatchKeyPressEvent
    )

    MaterialTheme {
        CommanderScreen(
            uiState = uiState,
            onEvent = viewModel::dispatch
        )
    }
}