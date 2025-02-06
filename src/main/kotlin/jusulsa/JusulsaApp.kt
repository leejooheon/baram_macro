package jusulsa

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import common.event.ObserveKeyEvents
import common.event.ObserveMouseEvents

@Composable
fun JusulsaApp() {
    val viewModel = remember { JusulsaViewModel() }

    ObserveKeyEvents(
        onReleased = viewModel::dispatchKeyReleaseEvent,
        onPressed = viewModel::dispatchKeyPressEvent
    )
    ObserveMouseEvents {
//        println("mouse: $it")
    }

    MaterialTheme {
        JusulsaScreen(
        )
    }
}