package jusulsa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import jusulsa.model.JusulsaDisplaySection
import jusulsa.model.JusulsaUiState

@Composable
internal fun JusulsaScreen(
    uiState: JusulsaUiState,
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(Color(0x88000000)) // ✅ 반투명 검은색 배경
    ) {
        LazyColumn(
            contentPadding = PaddingValues(all = 16.dp)
        ) {
            item {
                Text(
                    text = "${uiState.count}",
                    style = MaterialTheme.typography.h3,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(
                listOf(
                    uiState.addOnState,
                    uiState.resultState,
                )
            ) { model ->
                JusulsaDisplaySection(
                    texts = model.texts,
                    image = model.image,
                    rectangle = model.rectangle,
                    onRectangleChanged = {
//                        val event = UiEvent.OnRectangleChanged(
//                            rectangle = it,
//                            type = model.type
//                        )
//                        onEvent.invoke(event)
                    },
                    modifier = Modifier
                        .width(model.rectangle.width.dp)
                        .height(model.rectangle.height.dp)
                )
            }

        }
    }
}