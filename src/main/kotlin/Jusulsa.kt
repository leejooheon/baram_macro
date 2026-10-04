import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import common.event.RegisterNativeHook
import jusulsa.JusulsaApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "주술사",
        state = rememberWindowState(
            size = DpSize(400.dp, 460.dp),
            position = WindowPosition.PlatformDefault,
        ),
        alwaysOnTop = true,
    ) {
        RegisterNativeHook()
        JusulsaApp()
    }
}
