package follower

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*

@Composable
@Preview
fun FollowerApp() {
    val viewModel = remember { FollowerViewModel() }

    MaterialTheme {
        val uiState by viewModel.uiState.collectAsState()

        FollowerScreen(
            uiState = uiState,
            onEvent = viewModel::dispatch
        )
    }
}