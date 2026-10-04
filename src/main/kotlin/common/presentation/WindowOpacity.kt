package common.presentation

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import java.awt.Window

/**
 * 창 전체(제목 표시줄 포함)를 반투명하게 만든다.
 * AWT의 setOpacity는 제목 표시줄이 있는 창에서 쓸 수 없어서, Windows에서는 레이어드 창 속성을 직접 건다.
 */
fun Window.applyOpacity(opacity: Float) {
    val value = opacity.coerceIn(0.1f, 1f)
    val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    runCatching {
        if (isWindows) {
            val hwnd = WinDef.HWND(Pointer(Native.getWindowID(this)))
            val user32 = User32.INSTANCE
            val style = user32.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE)
            if (style and WinUser.WS_EX_LAYERED == 0) {
                user32.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, style or WinUser.WS_EX_LAYERED)
            }
            user32.SetLayeredWindowAttributes(hwnd, 0, (value * 255).toInt().toByte(), WinUser.LWA_ALPHA)
        } else {
            this.opacity = value
        }
    }.onFailure { println("창 투명도 설정 실패: ${it.message}") }
}
