import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import common.event.RegisterNativeHook
import follower.FollowerApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = WindowState(
            size = DpSize(500.dp, 700.dp),
            position = WindowPosition.PlatformDefault
        )
    ) {
        RegisterNativeHook()
        FollowerApp()
    }
}