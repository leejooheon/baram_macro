package follower

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import common.UiStateHolder

@Composable
fun FollowerApp() {
    val viewModel = remember { FollowerViewModel() }
    val uiState by UiStateHolder.state.collectAsState()
    val macroUiState by viewModel.macroUiState.collectAsState()

    MaterialTheme {
        FollowerScreen(
            uiState = uiState,
            macroUiState = macroUiState,
            onEvent = viewModel::dispatch
        )
    }
}