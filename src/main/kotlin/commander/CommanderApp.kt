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
import common.ObserveKeyEvents


@Composable
@Preview
fun CommanderApp() {
    val commander = remember { Commander() }
    val uiState by commander.uiState.collectAsState()

    ObserveKeyEvents(
        onReleased = commander::dispatchKeyReleaseEvent,
        onPressed = commander::dispatchKeyPressEvent
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyColumn {
                item {
                    ClientList(
                        clients = uiState.commanderState.clientHostAddressList
                    )
                }
                item {
                    CommandHistory(
                        histories = uiState.commandBuffer
                    )
                }

                item {
                    Button(
                        onClick = { commander.start() },
                        enabled = !uiState.isRunning,
                    ) {
                        Text("Start")
                    }
                }
            }
        }
    }
}