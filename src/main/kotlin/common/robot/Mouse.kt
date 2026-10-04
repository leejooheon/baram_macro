package common.robot

import com.sun.jna.Native
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.POINT
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.awt.event.InputEvent

/**
 * 게임 창 안의 한 점을 왼쪽 클릭한다. 클릭한 뒤 마우스는 원래 자리로 돌려놓는다.
 *
 * 좌표는 게임 창 클라이언트 영역의 실제 픽셀(캡처 이미지 좌표)이다.
 * 화면 좌표로 바꾸고 커서를 옮기는 건 Win32(ClientToScreen, SetCursorPos)로 해서
 * 윈도우 배율(150% 등)을 따로 계산하지 않는다. 누르고 떼는 건 키보드와 같이 Robot으로 보낸다.
 */
object Mouse {
    /** 커서를 옮긴 뒤 게임이 마우스 위치를 알아챌 때까지 기다리는 시간 */
    const val MOVE_DELAY = 15L

    @Suppress("FunctionName")
    private interface User32Ext : StdCallLibrary {
        fun ClientToScreen(hwnd: HWND, point: POINT): Boolean
        fun SetCursorPos(x: Int, y: Int): Boolean
        fun GetCursorPos(point: POINT): Boolean
    }

    private val user32 by lazy { Native.load("user32", User32Ext::class.java, W32APIOptions.DEFAULT_OPTIONS) }

    /** 키보드와 같은 입력 락 안에서 클릭한다. 화면 좌표로 못 바꾸면 false */
    suspend fun click(window: HWND, x: Int, y: Int, delay: Long = Keyboard.DEFAULT_DELAY): Boolean = Keyboard.atomic {
        val target = POINT(x, y)
        if (!user32.ClientToScreen(window, target)) return@atomic false
        val original = POINT().also { user32.GetCursorPos(it) }

        user32.SetCursorPos(target.x, target.y)
        try {
            delay(MOVE_DELAY)
            Keyboard.robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
            delay(delay)
        } finally {
            withContext(NonCancellable) {
                Keyboard.robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
                delay(delay)
                user32.SetCursorPos(original.x, original.y)
            }
        }
        true
    }
}
