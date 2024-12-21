package follower

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import commander.presentation.CommandHistory
import common.ObserveKeyEvents
import follower.presentation.ConnectionStateView
import follower.presentation.DisplayTest
import kotlinx.coroutines.launch


@Composable
@Preview
fun FollowerApp() {
    val follower = remember { Follower() }
    val uiState by follower.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    ObserveKeyEvents(
        onReleased = {
            scope.launch {
                follower.dispatchKeyReleaseEvent(it)
            }
        },
        onPressed = {
            scope.launch {
                follower.dispatchKeyPressEvent(it)
            }
        }
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyColumn {
                item {
                    ConnectionStateView(
                        state = uiState.connectionState
                    )
                }
                item {
                    CommandHistory(
                        histories = uiState.commandBuffer
                    )
                }
                item {
                    DisplayTest(
                        rectangle = uiState.buffStateRect,
                        onRectangleChange = {
                            follower.onBuffStateRectChanged(it)
                        }
                    )
                }
                item {
                    DisplayTest(
                        rectangle = uiState.magicRect,
                        onRectangleChange = {
                            follower.onMagicRectChanged(it)
                        }
                    )
                }
                item {
                    Button(
                        onClick = { follower.start() },
                        enabled = !uiState.isRunning,
                    ) {
                        Text("Start")
                    }
                }
            }
        }
    }
}