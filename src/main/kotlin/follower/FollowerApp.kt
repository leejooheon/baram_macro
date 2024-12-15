package follower

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import commander.presentation.CommandHistory
import follower.presentation.ConnectionStateView
import follower.presentation.DisplayTest


@Composable
@Preview
fun FollowerApp() {
    val follower = remember { Follower() }
    val uiState by follower.uiState.collectAsState()

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
                        rectangle = uiState.magicResultRect,
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