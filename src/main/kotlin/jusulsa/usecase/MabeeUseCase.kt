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
 * 마비. 내 상하좌우에 붙어서 나를 때릴 수 있는 몹 중 아직 마비를 안 건 몹을 마우스로 찍어 마비를 건다.
 * 멀리 있는 몹까지 다 걸면 마비만 하다가 공격을 못 하므로, 붙은 몹만 걸고 [MIN_INTERVAL_MILLIS]마다 한 번만 쓴다.
 *
 * 5매각을 만드는 중이면([reserved]가 있으면) 각 칸은 건너뛴다 (활력으로 풀어 준 몹을 다시 묶지 않게).
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
    private var lastCastAt = 0L

    override fun isReady(now: Long): Boolean {
        next = plan(now)
        return next != null
    }

    override suspend fun execute() {
        val (tile, aim) = next ?: return
        next = null
        if (SkillCaster.tryCast(Skill.MABEE, Target.Click(aim))) {
            lastCastAt = now()
            paralyzed[tile] = lastCastAt
        }
    }

    fun plan(now: Long): Pair<Tile, Aim>? {
        if (now - lastCastAt < MIN_INTERVAL_MILLIS) return null
        val detection = detection(now) ?: return null
        val grid = TileGrid.of(detection) ?: return null
        val monsters = detection.monsters.associateBy { grid.tileOf(it) }
        paralyzed.entries.removeIf { (tile, at) -> tile !in monsters || now - at > HOLD_MILLIS }

        val skip = reserved()
        val target = ME.neighbors.firstOrNull { it in monsters && it !in paralyzed && it !in skip } ?: return null
        reason.log("$target 에 마비 (마비 ${paralyzed.size}마리)")
        return target to Aim(detection.window, monsters.getValue(target).center())
    }

    companion object {
        private val ME = Tile(0, 0)
        /** 마비 사이에 공격할 틈을 두는 간격 */
        const val MIN_INTERVAL_MILLIS = 800L
        /** 마비가 풀렸다고 보는 시간. 실제 지속시간을 알면 맞춘다 */
        const val HOLD_MILLIS = 10_000L
    }
}
