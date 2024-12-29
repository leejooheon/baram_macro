import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import commander.CommanderApp
import common.event.RegisterNativeHook

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = WindowState(
            size = DpSize(450.dp, 450.dp),
            position = WindowPosition.PlatformDefault
        )
    ) {
        RegisterNativeHook()
        CommanderApp()
    }
}