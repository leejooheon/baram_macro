package follower

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import common.model.UiEvent
import common.model.UiState
import common.presentation.ConnectionStateItem
import common.presentation.DisplaySection
import follower.model.MacroUiState

@Composable
internal fun FollowerScreen(
    uiState: UiState,
    macroUiState: MacroUiState,
    onEvent: (UiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(all = 16.dp)
        ) {
            item {
                ConnectionStateItem(
                    state = uiState.connectionState
                )
            }

            item {
                MacroUiItem(macroUiState)
            }

            items(
                listOf(
                    uiState.xState,
                    uiState.yState,
                    uiState.buffState,
                    uiState.magicResultState,
                )
            ) { model ->
                DisplaySection(
                    model = model,
                    onRectangleChanged = {
                        val event = UiEvent.OnRectangleChanged(
                            rectangle = it,
                            type = model.type
                        )
                        onEvent.invoke(event)
                    },
                    modifier = Modifier
                        .width(model.rectangle.width.dp)
                        .height(model.rectangle.height.dp)
                )
            }

            item {
                Button(
                    onClick = { onEvent.invoke(UiEvent.OnTryConnect) },
                    enabled = !uiState.isRunning,
                ) {
                    Text("Start")
                }
            }
        }
    }
}

@Composable
private fun MacroUiItem(uiState: MacroUiState) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "이동: ${if(uiState.ctrlToggle) "OFF" else "ON"}",
            style = MaterialTheme.typography.body1.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "사이클 시간: ${uiState.cycleTime}",
            style = MaterialTheme.typography.body1.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Blue
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "목표 좌표: [${uiState.point.x}, ${uiState.point.y}]",
            style = MaterialTheme.typography.body1.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Preview
@Composable
private fun PreviewFollowerScreen() {
    MaterialTheme {
        FollowerScreen(
            uiState = UiState.default,
            macroUiState = MacroUiState.default,
            onEvent = {}
        )
    }
}