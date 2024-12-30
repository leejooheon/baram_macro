import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import common.event.RegisterNativeHook
import follower.FollowerApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(
            size = DpSize(500.dp, 700.dp),
            position = WindowPosition.PlatformDefault
        )
    ) {
        RegisterNativeHook()
        FollowerApp()
    }
}