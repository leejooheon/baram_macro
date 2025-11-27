import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import keyboard.KeyboardApp
import java.awt.Toolkit

fun main() = application {
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val screenWidth = screenSize.width
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(
            size = DpSize(screenWidth.dp, 400.dp),
            position = WindowPosition(0.dp, 620.dp)
        ),

        alwaysOnTop = true,
        undecorated = true,
        transparent = true,
        focusable = false,
        visible = true
    ) {
        LaunchedEffect(Unit) {
            window.focusableWindowState = false
            println("test: ${window.isFocusableWindow}")
        }
        KeyboardApp()
    }
}