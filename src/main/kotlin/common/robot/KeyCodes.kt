package common.robot

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import java.awt.event.KeyEvent

/** java KeyEvent.VK_*, Windows 가상 키코드, jnativehook VC_* 사이 변환 */
object KeyCodes {
    // java KeyEvent.VK_* 중 Windows VK 값과 다른 것들. 나머지(영문, 숫자, F키, 방향키 등)는 같다
    fun javaToWindowsVk(javaKeyCode: Int) = when (javaKeyCode) {
        KeyEvent.VK_ENTER -> 0x0D
        KeyEvent.VK_DELETE -> 0x2E
        KeyEvent.VK_INSERT -> 0x2D
        KeyEvent.VK_SEMICOLON -> 0xBA
        KeyEvent.VK_EQUALS -> 0xBB
        KeyEvent.VK_COMMA -> 0xBC
        KeyEvent.VK_MINUS -> 0xBD
        KeyEvent.VK_PERIOD -> 0xBE
        KeyEvent.VK_SLASH -> 0xBF
        KeyEvent.VK_BACK_QUOTE -> 0xC0
        KeyEvent.VK_OPEN_BRACKET -> 0xDB
        KeyEvent.VK_BACK_SLASH -> 0xDC
        KeyEvent.VK_CLOSE_BRACKET -> 0xDD
        KeyEvent.VK_QUOTE -> 0xDE
        else -> javaKeyCode
    }

    // 스캔코드로 보낼 때 확장 플래그가 필요한 키 (없으면 숫자패드 키로 인식된다)
    private val extendedVks = setOf(
        0x21, 0x22, 0x23, 0x24, // page up, page down, end, home
        0x25, 0x26, 0x27, 0x28, // 방향키
        0x2C, 0x2D, 0x2E, // print screen, insert, delete
        0x5B, 0x5C, 0x5D, // win, apps
        0x6F, 0x90, // 숫자패드 /, num lock
        0xA3, 0xA5, // 오른쪽 ctrl, alt
    )

    fun isExtended(windowsVk: Int) = windowsVk in extendedVks

    fun windowsVkToVc(windowsVk: Int): Int = vkToVc[windowsVk] ?: NativeKeyEvent.VC_UNDEFINED

    // libuiohook(jnativehook)의 Windows keycode_scancode_table과 같은 매핑. 기존 단축키 코드(VC_*)를 그대로 쓰기 위함
    private val vkToVc = mapOf(
        0x08 to NativeKeyEvent.VC_BACKSPACE,
        0x09 to NativeKeyEvent.VC_TAB,
        0x0C to NativeKeyEvent.VC_CLEAR,
        0x0D to NativeKeyEvent.VC_ENTER,
        0x10 to NativeKeyEvent.VC_SHIFT,
        0x11 to NativeKeyEvent.VC_CONTROL,
        0x12 to NativeKeyEvent.VC_ALT,
        0x13 to NativeKeyEvent.VC_PAUSE,
        0x14 to NativeKeyEvent.VC_CAPS_LOCK,
        0x15 to NativeKeyEvent.VC_KATAKANA,
        0x19 to NativeKeyEvent.VC_KANJI,
        0x1B to NativeKeyEvent.VC_ESCAPE,
        0x20 to NativeKeyEvent.VC_SPACE,
        0x21 to NativeKeyEvent.VC_PAGE_UP,
        0x22 to NativeKeyEvent.VC_PAGE_DOWN,
        0x23 to NativeKeyEvent.VC_END,
        0x24 to NativeKeyEvent.VC_HOME,
        0x25 to NativeKeyEvent.VC_LEFT,
        0x26 to NativeKeyEvent.VC_UP,
        0x27 to NativeKeyEvent.VC_RIGHT,
        0x28 to NativeKeyEvent.VC_DOWN,
        0x2C to NativeKeyEvent.VC_PRINTSCREEN,
        0x2D to NativeKeyEvent.VC_INSERT,
        0x2E to NativeKeyEvent.VC_DELETE,
        0x30 to NativeKeyEvent.VC_0,
        0x31 to NativeKeyEvent.VC_1,
        0x32 to NativeKeyEvent.VC_2,
        0x33 to NativeKeyEvent.VC_3,
        0x34 to NativeKeyEvent.VC_4,
        0x35 to NativeKeyEvent.VC_5,
        0x36 to NativeKeyEvent.VC_6,
        0x37 to NativeKeyEvent.VC_7,
        0x38 to NativeKeyEvent.VC_8,
        0x39 to NativeKeyEvent.VC_9,
        0x41 to NativeKeyEvent.VC_A,
        0x42 to NativeKeyEvent.VC_B,
        0x43 to NativeKeyEvent.VC_C,
        0x44 to NativeKeyEvent.VC_D,
        0x45 to NativeKeyEvent.VC_E,
        0x46 to NativeKeyEvent.VC_F,
        0x47 to NativeKeyEvent.VC_G,
        0x48 to NativeKeyEvent.VC_H,
        0x49 to NativeKeyEvent.VC_I,
        0x4A to NativeKeyEvent.VC_J,
        0x4B to NativeKeyEvent.VC_K,
        0x4C to NativeKeyEvent.VC_L,
        0x4D to NativeKeyEvent.VC_M,
        0x4E to NativeKeyEvent.VC_N,
        0x4F to NativeKeyEvent.VC_O,
        0x50 to NativeKeyEvent.VC_P,
        0x51 to NativeKeyEvent.VC_Q,
        0x52 to NativeKeyEvent.VC_R,
        0x53 to NativeKeyEvent.VC_S,
        0x54 to NativeKeyEvent.VC_T,
        0x55 to NativeKeyEvent.VC_U,
        0x56 to NativeKeyEvent.VC_V,
        0x57 to NativeKeyEvent.VC_W,
        0x58 to NativeKeyEvent.VC_X,
        0x59 to NativeKeyEvent.VC_Y,
        0x5A to NativeKeyEvent.VC_Z,
        0x5B to NativeKeyEvent.VC_META,
        0x5C to NativeKeyEvent.VC_META,
        0x5D to NativeKeyEvent.VC_CONTEXT_MENU,
        0x5F to NativeKeyEvent.VC_SLEEP,
        0x70 to NativeKeyEvent.VC_F1,
        0x71 to NativeKeyEvent.VC_F2,
        0x72 to NativeKeyEvent.VC_F3,
        0x73 to NativeKeyEvent.VC_F4,
        0x74 to NativeKeyEvent.VC_F5,
        0x75 to NativeKeyEvent.VC_F6,
        0x76 to NativeKeyEvent.VC_F7,
        0x77 to NativeKeyEvent.VC_F8,
        0x78 to NativeKeyEvent.VC_F9,
        0x79 to NativeKeyEvent.VC_F10,
        0x7A to NativeKeyEvent.VC_F11,
        0x7B to NativeKeyEvent.VC_F12,
        0x7C to NativeKeyEvent.VC_F13,
        0x7D to NativeKeyEvent.VC_F14,
        0x7E to NativeKeyEvent.VC_F15,
        0x7F to NativeKeyEvent.VC_F16,
        0x80 to NativeKeyEvent.VC_F17,
        0x81 to NativeKeyEvent.VC_F18,
        0x82 to NativeKeyEvent.VC_F19,
        0x83 to NativeKeyEvent.VC_F20,
        0x84 to NativeKeyEvent.VC_F21,
        0x85 to NativeKeyEvent.VC_F22,
        0x86 to NativeKeyEvent.VC_F23,
        0x87 to NativeKeyEvent.VC_F24,
        0x90 to NativeKeyEvent.VC_NUM_LOCK,
        0x91 to NativeKeyEvent.VC_SCROLL_LOCK,
        0xA0 to NativeKeyEvent.VC_SHIFT,
        0xA1 to NativeKeyEvent.VC_SHIFT,
        0xA2 to NativeKeyEvent.VC_CONTROL,
        0xA3 to NativeKeyEvent.VC_CONTROL,
        0xA4 to NativeKeyEvent.VC_ALT,
        0xA5 to NativeKeyEvent.VC_ALT,
        0xA6 to NativeKeyEvent.VC_BROWSER_BACK,
        0xA7 to NativeKeyEvent.VC_BROWSER_FORWARD,
        0xA8 to NativeKeyEvent.VC_BROWSER_REFRESH,
        0xA9 to NativeKeyEvent.VC_BROWSER_STOP,
        0xAA to NativeKeyEvent.VC_BROWSER_SEARCH,
        0xAB to NativeKeyEvent.VC_BROWSER_FAVORITES,
        0xAC to NativeKeyEvent.VC_BROWSER_HOME,
        0xAD to NativeKeyEvent.VC_VOLUME_MUTE,
        0xAE to NativeKeyEvent.VC_VOLUME_DOWN,
        0xAF to NativeKeyEvent.VC_VOLUME_UP,
        0xB0 to NativeKeyEvent.VC_MEDIA_NEXT,
        0xB1 to NativeKeyEvent.VC_MEDIA_PREVIOUS,
        0xB2 to NativeKeyEvent.VC_MEDIA_STOP,
        0xB3 to NativeKeyEvent.VC_MEDIA_PLAY,
        0xB5 to NativeKeyEvent.VC_MEDIA_SELECT,
        0xB6 to NativeKeyEvent.VC_APP_MAIL,
        0xB7 to NativeKeyEvent.VC_APP_CALCULATOR,
        0xBA to NativeKeyEvent.VC_SEMICOLON,
        0xBB to NativeKeyEvent.VC_EQUALS,
        0xBC to NativeKeyEvent.VC_COMMA,
        0xBD to NativeKeyEvent.VC_MINUS,
        0xBE to NativeKeyEvent.VC_PERIOD,
        0xBF to NativeKeyEvent.VC_SLASH,
        0xC0 to NativeKeyEvent.VC_BACKQUOTE,
        0xDB to NativeKeyEvent.VC_OPEN_BRACKET,
        0xDC to NativeKeyEvent.VC_BACK_SLASH,
        0xDD to NativeKeyEvent.VC_CLOSE_BRACKET,
        0xDE to NativeKeyEvent.VC_QUOTE,
        0xDF to NativeKeyEvent.VC_YEN,
        0xE5 to NativeKeyEvent.VC_APP_PICTURES,
        0xE6 to NativeKeyEvent.VC_APP_MUSIC,
        0xFE to NativeKeyEvent.VC_CLEAR,
    )
}
