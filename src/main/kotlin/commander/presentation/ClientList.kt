package commander.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ClientList(
    clients: List<String>
) {
    Text(
        text = "Client list",
        style = MaterialTheme.typography.h4
    )
    Spacer(modifier = Modifier.height(8.dp))
    if (clients.isEmpty()) {
        Text(
            text = "connection is empty",
            style = MaterialTheme.typography.body1
        )
    } else {
        clients.forEach {
            Text(
                text = "client: $it",
                style = MaterialTheme.typography.body1
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}