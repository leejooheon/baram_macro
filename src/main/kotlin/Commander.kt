import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import commander.CommanderApp
import common.event.RegisterNativeHook

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(
            size = DpSize(450.dp, 450.dp),
            position = WindowPosition.PlatformDefault
        )
    ) {
        RegisterNativeHook()
        CommanderApp()
    }
}