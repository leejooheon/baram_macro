import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import commander.CommanderApp
import common.event.RegisterNativeHook
import follower.FollowerApp

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        RegisterNativeHook()

        CommanderApp()
//        FollowerApp()
    }
}