package follower

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import common.UiStateHolder

@Composable
fun FollowerApp() {
    val viewModel = remember { FollowerViewModel() }
    val uiState by UiStateHolder.state.collectAsState()
    val commanderPoint by viewModel.commanderPoint.collectAsState()
    MaterialTheme {
        FollowerScreen(
            uiState = uiState,
            commanderPoint = commanderPoint,
            onEvent = viewModel::dispatch
        )
    }
}