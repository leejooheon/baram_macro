package jusulsa.engine

import common.robot.Keyboard
import common.robot.UserInput
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 매크로 실행 엔진. 루프 하나가 매번 상태를 보고 할 일 하나를 골라 실행한다.
 * 키보드는 하나뿐이라 여러 루프가 동시에 키를 보내면 서로 끼어들거나 한쪽이 굶는다. 그래서 고르는 곳을 한 군데로 모은다.
 *
 * 고르는 순서
 * 1. 사용자가 방향키로 이동 중이면 아무것도 안 한다
 * 2. [priority]에서 앞에서부터 할 일이 있는 첫 번째 (생존·버프처럼 늦으면 안 되는 것)
 * 3. 없으면 [rotation]을 돌아가며 할 일이 있는 다음 것 (공격처럼 계속 도는 것). 번갈아 고르므로 어느 하나가 굶지 않는다
 * 4. 아무것도 없으면 잠깐 쉬었다 다시 고른다
 */
class MacroEngine(
    private val priority: List<MacroUseCase>,
    private val rotation: List<MacroUseCase>,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var nextRotation = 0
    /** 직전에 우선 목록 일을 했는지. 했으면 다음 한 번은 공격에게 양보한다 */
    private var lastWasPriority = false
    private val counts = linkedMapOf<String, Int>()
    private var summaryAt = 0L

    suspend fun run() {
        while (currentCoroutineContext().isActive) {
            val moving = UserInput.waitMillis()
            if (moving > 0) {
                delay(moving)
                continue
            }

            val time = now()
            val ready = priority.firstOrNull { it.isReady(time) }
            // 힐·버프가 계속 할 일이 있어도 첨이 굶지 않게, 우선 목록 일을 한 뒤에는 공격에게 한 차례 양보한다
            val yielded = if (ready != null && lastWasPriority && !ready.neverYield) pickRotation(time) else null
            val urgent = if (yielded == null) ready else null
            val task = yielded ?: urgent ?: pickRotation(time)
            if (task == null) {
                delay(IDLE_MILLIS)
                continue
            }
            // 공격은 너무 자주라 생존·버프만 찍는다
            if (urgent != null && urgent.logEachRun) println("[MacroEngine] ${task.name}")
            lastWasPriority = urgent != null && urgent.logEachRun
            Keyboard.atomic { task.execute() }
            counts[task.name] = (counts[task.name] ?: 0) + 1
            summarize(time)
        }
    }

    /** 무엇을 몇 번 했는지 주기적으로 찍는다. 어떤 기능이 안 돌면 여기서 바로 보인다 */
    private fun summarize(time: Long) {
        if (summaryAt == 0L) summaryAt = time
        if (time - summaryAt < SUMMARY_MILLIS) return
        println("[MacroEngine] ${(time - summaryAt) / 1000}초간 " + counts.entries.joinToString { "${it.key} ${it.value}" })
        counts.clear()
        summaryAt = time
    }

    private fun pickRotation(time: Long): MacroUseCase? {
        for (i in rotation.indices) {
            val index = (nextRotation + i) % rotation.size
            if (rotation[index].isReady(time)) {
                nextRotation = index + 1
                return rotation[index]
            }
        }
        return null
    }

    companion object {
        /** 할 일이 없을 때 다시 고르기까지 쉬는 시간 */
        const val IDLE_MILLIS = 10L
        const val SUMMARY_MILLIS = 5_000L
    }
}
