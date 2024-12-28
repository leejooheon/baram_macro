package follower

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import common.event.UiEvent
import common.model.Type
import common.model.UiState
import common.presentation.ConnectionStateItem
import common.presentation.DisplaySection

@Composable
internal fun FollowerScreen(
    uiState: UiState,
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
                DisplaySection(
                    model = uiState.xState,
                    onRectangleChanged = {
                        val event = UiEvent.OnRectangleChanged(
                            rectangle = it,
                            type = Type.X
                        )
                        onEvent.invoke(event)
                    },
                    modifier = Modifier
                        .width(96.dp)
                        .height(32.dp)
                )
            }
            item {
                DisplaySection(
                    model = uiState.yState,
                    onRectangleChanged = {
                        val event = UiEvent.OnRectangleChanged(
                            rectangle = it,
                            type = Type.Y
                        )
                        onEvent.invoke(event)
                    },
                    modifier = Modifier
                        .width(96.dp)
                        .height(32.dp)
                )
            }

            item {
                DisplaySection(
                    model = uiState.buffState,
                    onRectangleChanged = {
                        val event = UiEvent.OnRectangleChanged(
                            rectangle = it,
                            type = Type.BUFF
                        )
                        onEvent.invoke(event)
                    },
                    modifier = Modifier
                        .width(256.dp)
                        .height(128.dp)
                )
            }

            item {
                DisplaySection(
                    model = uiState.magicResultState,
                    onRectangleChanged = {
                        val event = UiEvent.OnRectangleChanged(
                            rectangle = it,
                            type = Type.MAGIC_RESULT
                        )
                        onEvent.invoke(event)
                    },
                    modifier = Modifier
                        .width(256.dp)
                        .height(58.dp)
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