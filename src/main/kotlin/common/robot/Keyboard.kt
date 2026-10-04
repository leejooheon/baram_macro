package common.robot

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.awt.Robot
import java.awt.event.InputEvent
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

object Keyboard {
    val robot = Robot()

    /**
     * 키를 누르고 있는 시간이자 뗀 뒤 다음 키까지의 기본 간격(ms).
     * 10ms는 한 프레임(60fps 기준 16ms)보다 짧아서 게임이 입력을 놓칠 수 있다.
     * 그래도 씹히면 이 값을 늘린다.
     */
    const val DEFAULT_DELAY = 25L

    private val heldKeys = ConcurrentHashMap.newKeySet<Int>()

    // 모든 매크로가 공유하는 입력 락. 여러 매크로가 동시에 돌아도 키 조합이 섞이지 않는다
    private val inputLock = Mutex()

    private class LockOwner : AbstractCoroutineContextElement(LockOwner) {
        companion object Key : CoroutineContext.Key<LockOwner>
    }

    /**
     * block 안의 키 입력을 다른 매크로가 끼어들지 못하게 한 번에 보낸다.
     * 이미 락을 잡은 코루틴 안에서 다시 부르면 그대로 실행한다(재진입 가능).
     */
    suspend fun <T> atomic(block: suspend () -> T): T {
        if (currentCoroutineContext()[LockOwner] != null) return block()
        return inputLock.withLock {
            withContext(LockOwner()) { block() }
        }
    }

    suspend fun pressKeyRepeatedly(keyEvent: Int, time: Int, delay: Long = DEFAULT_DELAY) {
        repeat(time) {
            pressAndRelease(keyEvent, delay)
        }
    }

    suspend fun pressAndRelease(keyEvent: Int, delay: Long = DEFAULT_DELAY) = atomic {
        try {
            press(keyEvent)
            delay(delay)
        } finally {
            withContext(NonCancellable) {
                release(keyEvent)
                delay(delay)
            }
        }
    }

    fun press(keyEvent: Int) {
        synchronized(robot) {
            InjectedKeys.register(keyEvent, pressed = true)
            robot.keyPress(keyEvent)
            heldKeys.add(keyEvent)
        }
    }

    fun release(keyEvent: Int) {
        synchronized(robot) {
            InjectedKeys.register(keyEvent, pressed = false)
            robot.keyRelease(keyEvent)
            heldKeys.remove(keyEvent)
        }
    }

    /** 매크로가 중간에 취소돼도 shift 같은 키가 눌린 채로 남지 않게 한다 */
    fun releaseAll() {
        heldKeys.toList().forEach { release(it) }
    }

    suspend fun mouseClick(
        x: Float,
        y: Float,
        button: Int = InputEvent.BUTTON1_DOWN_MASK
    ) = atomic {
        synchronized(robot) { robot.mouseMove(x.toInt(), y.toInt()) }
        mouseClick(button)
    }

    suspend fun mouseClick(
        button: Int = InputEvent.BUTTON1_DOWN_MASK
    ) = atomic {
        synchronized(robot) { robot.mousePress(button) }
        try {
            delay(20)
        } finally {
            synchronized(robot) { robot.mouseRelease(button) }
        }
    }
}
