package common.robot

import java.awt.event.KeyEvent

/**
 * Robot으로 보낸 키를 기록해 두었다가, 전역 키 훅(JNativeHook)이 그 키를 다시 받았을 때 걸러낸다.
 *
 * Windows 저수준 훅은 SendInput으로 주입된 키도 사용자 입력과 똑같이 전달한다.
 * 이걸 거르지 않으면 매크로가 누른 1, 2, 3, ESC, 방향키가 다시 단축키로 인식돼서
 * 다른 매크로가 실행되거나(헬파이어의 1 -> 힐 매크로) 실행 중인 매크로가 취소된다(ESC).
 */
object InjectedKeys {
    private const val EXPIRE_MS = 1_000L

    // key: Windows 가상 키코드 * 2 + (눌림이면 1), value: 주입한 시각들
    private val pending = HashMap<Int, ArrayDeque<Long>>()

    @Synchronized
    fun register(javaKeyCode: Int, pressed: Boolean) {
        val now = System.currentTimeMillis()
        val queue = pending.getOrPut(slot(toWindowsVk(javaKeyCode), pressed)) { ArrayDeque() }
        queue.dropExpired(now)
        queue.addLast(now)
    }

    /** 훅으로 들어온 이벤트가 우리가 주입한 것이면 true를 돌려주고 기록에서 하나 지운다 */
    @Synchronized
    fun consume(rawCode: Int, pressed: Boolean): Boolean {
        val now = System.currentTimeMillis()
        val queue = pending[slot(normalize(rawCode), pressed)] ?: return false
        queue.dropExpired(now)
        return queue.removeFirstOrNull() != null
    }

    private fun ArrayDeque<Long>.dropExpired(now: Long) {
        while (isNotEmpty() && now - first() > EXPIRE_MS) removeFirst()
    }

    private fun slot(vk: Int, pressed: Boolean) = vk * 2 + if (pressed) 1 else 0

    // 훅은 좌/우 modifier를 구분해서 알려준다
    private fun normalize(rawCode: Int) = when (rawCode) {
        0xA0, 0xA1 -> 0x10 // shift
        0xA2, 0xA3 -> 0x11 // ctrl
        0xA4, 0xA5 -> 0x12 // alt
        else -> rawCode
    }

    // java KeyEvent.VK_* 중 Windows VK 값과 다른 것들. 나머지(영문, 숫자, F키, 방향키 등)는 같다
    private fun toWindowsVk(javaKeyCode: Int) = when (javaKeyCode) {
        KeyEvent.VK_ENTER -> 0x0D
        KeyEvent.VK_DELETE -> 0x2E
        KeyEvent.VK_INSERT -> 0x2D
        KeyEvent.VK_SEMICOLON -> 0xBA
        KeyEvent.VK_EQUALS -> 0xBB
        KeyEvent.VK_COMMA -> 0xBC
        KeyEvent.VK_MINUS -> 0xBD
        KeyEvent.VK_PERIOD -> 0xBE
        KeyEvent.VK_SLASH -> 0xBF
        KeyEvent.VK_OPEN_BRACKET -> 0xDB
        KeyEvent.VK_BACK_SLASH -> 0xDC
        KeyEvent.VK_CLOSE_BRACKET -> 0xDD
        else -> javaKeyCode
    }
}
