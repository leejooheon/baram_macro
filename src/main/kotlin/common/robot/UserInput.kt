package common.robot

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import java.util.concurrent.ConcurrentHashMap

/**
 * 사용자가 실제로 누른 방향키(매크로가 보낸 키 제외)를 추적한다.
 * 사용자가 이동 중일 때 방향으로 대상을 잡는 마법은 대상이 틀어질 수 있어서, 잠깐 미루는 데 쓴다.
 */
object UserInput {
    private val ARROWS = setOf(
        NativeKeyEvent.VC_UP,
        NativeKeyEvent.VC_LEFT,
        NativeKeyEvent.VC_DOWN,
        NativeKeyEvent.VC_RIGHT,
    )

    /** 방향키를 뗀 뒤에도 게임이 이동을 마칠 때까지 이만큼 기다린다 */
    const val SETTLE_MILLIS = 150L

    private val heldArrows = ConcurrentHashMap.newKeySet<Int>()
    @Volatile var lastArrowAt: Long = 0L
        private set

    fun onKey(keyCode: Int, pressed: Boolean) {
        if (keyCode !in ARROWS) return
        if (pressed) heldArrows.add(keyCode) else heldArrows.remove(keyCode)
        lastArrowAt = System.currentTimeMillis()
    }

    /** 사용자가 방향키를 누르고 있거나 막 뗐으면 true */
    fun isMoving(now: Long = System.currentTimeMillis()): Boolean =
        heldArrows.isNotEmpty() || now - lastArrowAt < SETTLE_MILLIS

    /** 이동이 끝날 때까지 남은 시간(ms). 누르고 있으면 다시 확인할 간격을 돌려준다 */
    fun waitMillis(now: Long = System.currentTimeMillis()): Long = when {
        heldArrows.isNotEmpty() -> 30L
        else -> (lastArrowAt + SETTLE_MILLIS - now).coerceAtLeast(0)
    }
}
