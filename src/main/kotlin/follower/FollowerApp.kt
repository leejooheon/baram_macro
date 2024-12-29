package follower

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import common.UiStateHolder

@Composable
@Preview
fun FollowerApp() {
    val viewModel = remember { FollowerViewModel() }
    val uiState by UiStateHolder.state.collectAsState()

    MaterialTheme {
        FollowerScreen(
            uiState = uiState,
            onEvent = viewModel::dispatch
        )
    }
}