package commander.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import commander.model.commandToString

@Composable
internal fun CommandHistory(
    histories: List<String>
) {
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Command history",
        style = MaterialTheme.typography.h4
    )

    Spacer(modifier = Modifier.height(8.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp),
    ) {
        Card(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalGrid(
                state = rememberLazyGridState(),
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(histories) { index, history ->
                    Text(
                        text = "$index: $history",
                        style = MaterialTheme.typography.body1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}