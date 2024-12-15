package follower.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import follower.model.ConnectionState

@Composable
internal fun ConnectionStateView(
    state: ConnectionState
) {
    Text(
        text = "Connection state",
        style = MaterialTheme.typography.h4
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = state.toString(),
        style = MaterialTheme.typography.body1
    )
    Spacer(modifier = Modifier.height(8.dp))
}