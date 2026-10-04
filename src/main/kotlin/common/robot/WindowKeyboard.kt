package common.robot

import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.ptr.IntByReference
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 특정 창(HWND)에 PostMessage로 키를 보낸다. 창이 앞에 없어도 그 창에만 들어간다.
 *
 * 게임이 키를 윈도우 메시지(WM_KEYDOWN)로 읽을 때만 동작한다.
 * DirectInput이나 GetAsyncKeyState로 읽는 게임은 무시하고, shift 같은 조합키 상태도 전달되지 않을 수 있다.
 */
object WindowKeyboard {
    private const val WM_KEYDOWN = 0x0100
    private const val WM_KEYUP = 0x0101

    fun foregroundWindow(): WinDef.HWND? = User32.INSTANCE.GetForegroundWindow()

    /** 로그용: 창 제목, 클래스, 프로세스 ID */
    fun describe(window: WinDef.HWND): String {
        val title = CharArray(256)
        User32.INSTANCE.GetWindowText(window, title, title.size)
        val className = CharArray(256)
        User32.INSTANCE.GetClassName(window, className, className.size)
        val pid = IntByReference()
        User32.INSTANCE.GetWindowThreadProcessId(window, pid)
        return "title=${String(title).trimEnd('\u0000')}, class=${String(className).trimEnd('\u0000')}, pid=${pid.value}"
    }

    fun press(window: WinDef.HWND, javaKeyCode: Int) = post(window, javaKeyCode, pressed = true)

    fun release(window: WinDef.HWND, javaKeyCode: Int) = post(window, javaKeyCode, pressed = false)

    suspend fun pressAndRelease(window: WinDef.HWND, javaKeyCode: Int, delay: Long = Keyboard.DEFAULT_DELAY) {
        try {
            press(window, javaKeyCode)
            delay(delay)
        } finally {
            withContext(NonCancellable) {
                release(window, javaKeyCode)
                delay(delay)
            }
        }
    }

    private fun post(window: WinDef.HWND, javaKeyCode: Int, pressed: Boolean) {
        val vk = KeyCodes.javaToWindowsVk(javaKeyCode)
        // lParam: 반복 횟수 1, 16~23비트 스캔코드, 24비트 확장키, 30~31비트 이전 상태/뗌
        var lParam = 1L or (WinInput.scanCode(vk).toLong() shl 16)
        if (KeyCodes.isExtended(vk)) lParam = lParam or (1L shl 24)
        if (!pressed) lParam = lParam or (1L shl 30) or (1L shl 31)

        val message = if (pressed) WM_KEYDOWN else WM_KEYUP
        User32.INSTANCE.PostMessage(window, message, WinDef.WPARAM(vk.toLong()), WinDef.LPARAM(lParam))
    }
}
