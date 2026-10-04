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
 * 5매각을 만드는 중인 몹([reserved])은 건너뛴다 (마비가 걸리면 각으로 못 온다).
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
        val target = monsters.keys
            .filter { it !in paralyzed && it !in skip && distance(it) <= RANGE * RANGE }
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
