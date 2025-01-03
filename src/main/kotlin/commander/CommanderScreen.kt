package commander

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

@Composable
internal fun CommanderScreen(
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
                DetectionTimeItem(uiState.ocrDetectionTime)
            }

            items(
                listOf(
                    uiState.xState,
                    uiState.yState,
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
private fun DetectionTimeItem(
    detectionTime: Long
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "통신 시간: $detectionTime",
                style = MaterialTheme.typography.body1.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Green
                )
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}


@Preview
@Composable
private fun PreviewCommanderScreen() {
    MaterialTheme {
        CommanderScreen(
            uiState = UiState.default,
            onEvent = {}
        )
    }
}