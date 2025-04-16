import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import keyboard.KeyboardApp
import java.awt.Toolkit
import com.sun.jna.*
import com.sun.jna.win32.*

fun main() = application {
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val screenWidth = screenSize.width
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(
            size = DpSize((screenWidth - 50).dp, 300.dp),
//            position = WindowPosition(1267.dp, 260.dp) // 화면 상단 좌측 고정
        ),

        alwaysOnTop = true,
        undecorated = true,
        transparent = true,
        visible = true,
        focusable = false
    ) {
        LaunchedEffect(Unit) {
            window.focusableWindowState = false
            println("test: ${window.isFocusableWindow}")
        }
        KeyboardApp()
    }
}
// --- Win32 native 인터페이스 ---
interface User32 : StdCallLibrary {
    companion object {
        val INSTANCE: User32 = Native.load("user32", User32::class.java)
        const val SWP_NOMOVE = 0x0001
        const val SWP_NOSIZE = 0x0002
        const val SWP_NOZORDER = 0x0004
        const val SWP_FRAMECHANGED = 0x0020
    }

    fun GetWindowLong(hWnd: HWND, nIndex: Int): Int
    fun SetWindowLong(hWnd: HWND, nIndex: Int, dwNewLong: Int): Int
    fun SetWindowPos(hWnd: HWND, hWndInsertAfter: HWND?, X: Int, Y: Int, cx: Int, cy: Int, uFlags: Int): Boolean
}
class HWND(p: Pointer) : WinNT.HANDLE(p)
fun applyNoActivateStyle(window: java.awt.Window) {
    val hwnd = HWND(Native.getComponentPointer(window))

    val user32 = User32.INSTANCE
    val GWL_EXSTYLE = -20
    val WS_EX_NOACTIVATE = 0x08000000
    val WS_EX_TOOLWINDOW = 0x00000080
    val WS_EX_TOPMOST = 0x00000008

    val currentStyle = user32.GetWindowLong(hwnd, GWL_EXSTYLE)
    val newStyle = currentStyle or WS_EX_NOACTIVATE or WS_EX_TOOLWINDOW or WS_EX_TOPMOST

    user32.SetWindowLong(hwnd, GWL_EXSTYLE, newStyle)
    user32.SetWindowPos(
        hwnd, null, 0, 0, 0, 0,
        User32.SWP_NOMOVE or User32.SWP_NOSIZE or User32.SWP_NOZORDER or User32.SWP_FRAMECHANGED
    )
}
