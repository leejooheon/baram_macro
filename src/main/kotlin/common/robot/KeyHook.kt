package common.robot

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/**
 * Windows 저수준 키보드 훅(WH_KEYBOARD_LL)을 직접 건다.
 *
 * - 프로그램이 보낸 키(LLKHF_INJECTED, 매크로의 Robot 입력 포함)는 단축키로 처리하지 않는다.
 * - consumeKeys에 넣은 키는 게임으로 넘기지 않고 삼킨다(예: 1을 단축키로 쓰면 게임에는 1이 안 들어간다).
 * - 리스너는 별도 스레드에서 순서대로 호출한다. 훅 콜백이 오래 걸리면 Windows가 훅을 몰래 떼어버리기 때문.
 *
 * 키 코드는 jnativehook의 VC_* 값으로 넘겨서 기존 단축키 코드를 그대로 쓸 수 있다.
 */
object KeyHook {
    interface Listener {
        val consumeKeys: Set<Int>
        fun onKey(keyCode: Int, pressed: Boolean)
    }

    private const val WM_KEYDOWN = 0x0100
    private const val WM_KEYUP = 0x0101
    private const val WM_SYSKEYDOWN = 0x0104
    private const val WM_SYSKEYUP = 0x0105
    private const val LLKHF_INJECTED = 0x10

    val isAvailable: Boolean = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    private val listeners = CopyOnWriteArrayList<Listener>()
    private val dispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "key-hook-dispatch").apply { isDaemon = true }
    }

    @Volatile private var hook: WinUser.HHOOK? = null
    private var started = false

    // 콜백 객체가 GC되면 훅이 죽으므로 필드로 잡아둔다
    private val proc = WinUser.LowLevelKeyboardProc { nCode, wParam, info ->
        if (nCode >= 0 && handle(wParam.toInt(), info)) {
            WinDef.LRESULT(1)
        } else {
            User32.INSTANCE.CallNextHookEx(hook, nCode, wParam, WinDef.LPARAM(Pointer.nativeValue(info.pointer)))
        }
    }

    fun addListener(listener: Listener) {
        start()
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    /** true를 돌려주면 키를 삼킨다 */
    private fun handle(message: Int, info: WinUser.KBDLLHOOKSTRUCT): Boolean {
        val pressed = when (message) {
            WM_KEYDOWN, WM_SYSKEYDOWN -> true
            WM_KEYUP, WM_SYSKEYUP -> false
            else -> return false
        }
        // 매크로가 보낸 키는 그대로 게임에 넘긴다
        if (info.flags and LLKHF_INJECTED != 0) return false

        val keyCode = KeyCodes.windowsVkToVc(info.vkCode)
        if (keyCode == NativeKeyEvent.VC_UNDEFINED) return false

        dispatcher.execute {
            listeners.forEach { it.onKey(keyCode, pressed) }
        }
        return listeners.any { keyCode in it.consumeKeys }
    }

    @Synchronized
    private fun start() {
        if (started) return
        started = true
        thread(name = "key-hook", isDaemon = true) {
            val module = Kernel32.INSTANCE.GetModuleHandle(null)
            hook = User32.INSTANCE.SetWindowsHookEx(WinUser.WH_KEYBOARD_LL, proc, module, 0)
            if (hook == null) {
                println("키보드 훅 설치 실패: ${Kernel32.INSTANCE.GetLastError()}")
                return@thread
            }
            // 저수준 훅은 훅을 건 스레드에 메시지 루프가 돌아야 호출된다
            val msg = WinUser.MSG()
            while (User32.INSTANCE.GetMessage(msg, null, 0, 0) > 0) {
                User32.INSTANCE.TranslateMessage(msg)
                User32.INSTANCE.DispatchMessage(msg)
            }
            User32.INSTANCE.UnhookWindowsHookEx(hook)
        }
    }
}
