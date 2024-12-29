package follower

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import common.UiStateHolder

@Preview
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