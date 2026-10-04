package jusulsa.engine

import common.robot.Keyboard

/**
 * 매크로가 하는 일 하나. [MacroEngine]이 상태를 보고 고른 뒤 실행한다.
 *
 * - [isReady]는 OcrStateHolder, 시전 기록 같은 상태만 보고 정한다. 키를 누르거나 기다리지 않는다.
 * - [execute]는 키 입력 한 묶음만 보낸다. 엔진이 입력 락을 잡은 채로 부르므로 중간에 다른 입력이 끼지 않는다.
 *   기다리지 않고 바로 끝나야 다음 할 일을 다시 고를 수 있다.
 */
interface MacroUseCase {
    /** 로그에 찍을 이름 */
    val name: String

    fun isReady(now: Long): Boolean

    suspend fun execute()

    /** 엔진 밖(단축키 등)에서 한 번 쓸 때. 할 일이 있었으면 true */
    suspend operator fun invoke(): Boolean {
        if (!isReady(System.currentTimeMillis())) return false
        Keyboard.atomic { execute() }
        return true
    }
}

/** 판단 이유가 바뀔 때만 찍는다. 엔진이 자주 물어봐도 로그가 넘치지 않게 */
class ReasonLog(private val tag: String) {
    @Volatile private var last: String? = null

    fun log(message: String) {
        if (message == last) return
        last = message
        println("[$tag] $message")
    }
}
