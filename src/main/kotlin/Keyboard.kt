import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import keyboard.KeyboardApp
import kotlinx.coroutines.awaitCancellation
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.Window
import javax.swing.JWindow
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.system.exitProcess

//fun main() = application {
//    val screenSize = Toolkit.getDefaultToolkit().screenSize
//    val screenWidth = screenSize.width
//    Window(
//        onCloseRequest = ::exitApplication,
//        state = rememberWindowState(
//            size = DpSize((screenWidth - 50).dp, 300.dp),
////            position = WindowPosition(1267.dp, 260.dp) // 화면 상단 좌측 고정
//        ),
//
//        alwaysOnTop = true,
//        undecorated = true,
//        transparent = true,
//        visible = true,
//        focusable = false
//    ) {
//        LaunchedEffect(Unit) {
//            window.type = Window.Type.UTILITY
//            window.focusableWindowState = false
//            println("test: ${window.isFocusableWindow}")
//        }
//        KeyboardApp()
//    }
//}
fun main() {
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val screenWidth = screenSize.width
    SwingUtilities.invokeLater {
        val panel = ComposePanel().apply {
            setContent { KeyboardApp() }
            isFocusable = false
            isRequestFocusEnabled = false
        }

        val window = JWindow().apply {
            type = Window.Type.UTILITY
            isFocusable = false
            focusableWindowState = false
            isAutoRequestFocus = false
            rootPane.isFocusable = false
            isAlwaysOnTop = true
            setSize(screenSize.width - 50, 300) // 크기 설정
            setLocation(25, screenSize.height - 320) // 화면 하단 위치
            contentPane.add(panel)
        }

        window.isVisible = true
    }
    while (true) {
        Thread.sleep(1000)
    }
}