package common.robot

import com.sun.jna.Native
import com.sun.jna.platform.win32.BaseTSD
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.win32.StdCallLibrary

/**
 * Windows SendInput으로 키를 보낸다.
 *
 * java Robot은 가상 키코드만 보내는데, 게임은 실제 키보드처럼 스캔코드로 읽는 경우가 많다.
 * 여기서는 스캔코드로 보내고, dwExtraInfo에 표시를 남겨 우리 훅(KeyHook)이 자기 입력을 정확히 걸러낼 수 있게 한다.
 */
object WinInput {
    /** 이 앱이 보낸 입력이라는 표시 ("BARM") */
    const val MARKER = 0x4241524DL

    val isAvailable: Boolean = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    private const val KEYEVENTF_EXTENDEDKEY = 0x0001
    private const val KEYEVENTF_KEYUP = 0x0002
    private const val KEYEVENTF_SCANCODE = 0x0008
    private const val MAPVK_VK_TO_VSC = 0

    private interface User32Ext : StdCallLibrary {
        fun MapVirtualKeyW(uCode: Int, uMapType: Int): Int
    }

    private val user32Ext by lazy { Native.load("user32", User32Ext::class.java) }

    fun sendKey(javaKeyCode: Int, pressed: Boolean): Boolean {
        val vk = KeyCodes.javaToWindowsVk(javaKeyCode)
        val scan = user32Ext.MapVirtualKeyW(vk, MAPVK_VK_TO_VSC)

        var flags = if (pressed) 0 else KEYEVENTF_KEYUP
        if (KeyCodes.isExtended(vk)) flags = flags or KEYEVENTF_EXTENDEDKEY
        // 스캔코드를 못 찾는 키는 가상 키코드로 보낸다
        if (scan != 0) flags = flags or KEYEVENTF_SCANCODE

        val input = WinUser.INPUT()
        input.type = WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD.toLong())
        input.input.setType("ki")
        input.input.ki.wVk = WinDef.WORD(vk.toLong())
        input.input.ki.wScan = WinDef.WORD(scan.toLong())
        input.input.ki.dwFlags = WinDef.DWORD(flags.toLong())
        input.input.ki.time = WinDef.DWORD(0)
        input.input.ki.dwExtraInfo = BaseTSD.ULONG_PTR(MARKER)

        val sent = User32.INSTANCE.SendInput(WinDef.DWORD(1), arrayOf(input), input.size())
        return sent.toInt() == 1
    }
}
