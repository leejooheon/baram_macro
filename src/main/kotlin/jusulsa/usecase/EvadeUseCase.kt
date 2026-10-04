package jusulsa.usecase

import common.robot.Keyboard
import detector.Detection
import detector.DetectionStateHolder
import detector.Tile
import detector.TileGrid
import jusulsa.engine.MacroUseCase
import jusulsa.engine.ReasonLog
import java.awt.event.KeyEvent

/**
 * 몹 피하기. 내 상하좌우 칸에 몹이 붙으면 몹이 없는 쪽으로 한 칸 움직인다.
 * 갈 칸은 "그 칸 옆에 붙는 몹 수"가 가장 적은 곳, 같으면 가장 가까운 몹에서 먼 곳.
 * 지금 자리보다 나아지지 않으면 움직이지 않는다 (벽, 몹에 둘러싸임).
 *
 * 몹 탐지가 꺼져 있거나 결과가 오래됐으면 아무것도 안 한다. 맵 이동은 사용자가 한다.
 */
class EvadeUseCase(
    private val detection: (now: Long) -> Detection? = { DetectionStateHolder.state.value?.takeIf { d -> d.isFresh(it) } },
    private val now: () -> Long = System::currentTimeMillis,
) : MacroUseCase {
    override val name = "몹 피하기"
    private val reason = ReasonLog("EvadeUseCase")
    private var lastMoveAt = 0L
    private var next: Int? = null

    override fun isReady(now: Long): Boolean {
        next = if (now - lastMoveAt < MOVE_INTERVAL_MILLIS) null else plan(now)
        return next != null
    }

    override suspend fun execute() {
        val key = next ?: return
        next = null
        Keyboard.pressAndRelease(key)
        lastMoveAt = now()
    }

    /** 누를 방향키. 피할 필요가 없거나 피할 곳이 없으면 null */
    fun plan(now: Long): Int? {
        val detection = detection(now) ?: return null
        // 이동한 뒤의 화면으로 다시 판단해야 하므로, 움직인 다음에 찍은 결과만 쓴다
        if (detection.capturedAt <= lastMoveAt) return null
        val grid = TileGrid.of(detection) ?: return null
        val monsters = detection.monsters.map { grid.tileOf(it) }.toSet()

        fun adjacent(tile: Tile) = tile.neighbors.count { it in monsters }
        fun nearest(tile: Tile) = monsters.minOfOrNull { (it.x - tile.x) * (it.x - tile.x) + (it.y - tile.y) * (it.y - tile.y) } ?: Int.MAX_VALUE

        val here = adjacent(ME)
        if (here == 0) return null

        val best = MOVES.entries
            .filter { (tile, _) -> tile !in monsters }
            .minWithOrNull(compareBy<Map.Entry<Tile, Int>> { adjacent(it.key) }.thenByDescending { nearest(it.key) })
        if (best == null || adjacent(best.key) >= here) {
            reason.log("몹 ${here}마리 붙음, 피할 곳 없음")
            return null
        }
        reason.log("몹 ${here}마리 붙음 -> ${best.key}로 피함")
        return best.value
    }

    companion object {
        private val ME = Tile(0, 0)
        private val MOVES = mapOf(
            Tile(0, -1) to KeyEvent.VK_UP,
            Tile(0, 1) to KeyEvent.VK_DOWN,
            Tile(-1, 0) to KeyEvent.VK_LEFT,
            Tile(1, 0) to KeyEvent.VK_RIGHT,
        )
        /** 한 칸 걷는 동안은 다시 움직이지 않는다 */
        const val MOVE_INTERVAL_MILLIS = 400L
    }
}
