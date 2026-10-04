package jusulsa.usecase

import detector.Aim
import detector.Detection
import detector.DetectionStateHolder
import detector.Tile
import detector.TileGrid
import detector.center
import jusulsa.engine.MacroUseCase
import jusulsa.engine.ReasonLog
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.Target

/**
 * 마비 뿌리기. 나와 [RANGE]칸 안에 있는 몹 중 아직 마비를 안 건 몹을 가까운 순서로 마우스로 찍어 마비를 건다.
 * 몹이 못 따라오니 피하고 움직이기 쉬워진다.
 *
 * 사냥 영상처럼 "마비를 일단 다 돌리고 하나씩 조정"한다. 5매각을 만드는 중이면([reserved]가 있으면)
 * 각 칸은 건너뛰고 내 옆에 붙은 몹만 건다 (활력으로 풀어 준 몹을 다시 묶지 않게).
 * 마비가 걸렸는지는 화면에 안 보이므로 건 칸과 시각을 기억하고, 그 칸에서 몹이 사라지거나 [HOLD_MILLIS]가 지나면 다시 건다.
 */
class MabeeUseCase(
    private val reserved: () -> Set<Tile> = { emptySet() },
    private val detection: (now: Long) -> Detection? = { DetectionStateHolder.state.value?.takeIf { d -> d.isFresh(it) } },
    private val now: () -> Long = System::currentTimeMillis,
) : MacroUseCase {
    override val name = "마비"
    private val reason = ReasonLog("MabeeUseCase")
    private val paralyzed = mutableMapOf<Tile, Long>()

    /** 마비를 걸어 둔 칸. 몹 피하기는 이 몹들을 무시한다 (못 움직이고 못 때린다) */
    val paralyzedTiles: Set<Tile> get() = paralyzed.keys.toSet()
    private var next: Pair<Tile, Aim>? = null

    override fun isReady(now: Long): Boolean {
        next = plan(now)
        return next != null
    }

    override suspend fun execute() {
        val (tile, aim) = next ?: return
        next = null
        if (SkillCaster.tryCast(Skill.MABEE, Target.Click(aim))) paralyzed[tile] = now()
    }

    fun plan(now: Long): Pair<Tile, Aim>? {
        val detection = detection(now) ?: return null
        val grid = TileGrid.of(detection) ?: return null
        val monsters = detection.monsters.associateBy { grid.tileOf(it) }
        paralyzed.entries.removeIf { (tile, at) -> tile !in monsters || now - at > HOLD_MILLIS }

        val skip = reserved()
        val range = if (skip.isEmpty()) RANGE else 1
        val target = monsters.keys
            .filter { it !in paralyzed && it !in skip && distance(it) <= range * range }
            .minByOrNull { distance(it) }
            ?: return null
        reason.log("$target 에 마비 (마비 ${paralyzed.size}마리)")
        return target to Aim(detection.window, monsters.getValue(target).center())
    }

    private fun distance(tile: Tile) = tile.x * tile.x + tile.y * tile.y

    companion object {
        /** 이 칸 수 안의 몹만 마비를 건다 */
        const val RANGE = 4
        /** 마비가 풀렸다고 보는 시간. 실제 지속시간을 알면 맞춘다 */
        const val HOLD_MILLIS = 10_000L
    }
}
