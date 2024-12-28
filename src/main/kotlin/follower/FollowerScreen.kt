package follower

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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