package commander

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import commander.presentation.ClientList
import commander.presentation.CommandHistory
import common.event.ObserveKeyEvents
import common.event.ObserveMouseEvents

const val PORT = "23456"

@Composable
@Preview
fun CommanderApp() {
    val viewModel = remember { CommanderViewModel() }
    val uiState by viewModel.uiState.collectAsState()

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