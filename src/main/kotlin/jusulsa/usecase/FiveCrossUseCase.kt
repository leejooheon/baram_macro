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
import ocr.OcrStateHolder
import ocr.model.TimerRegion

/**
 * 5매각: 중심 몹과 그 상하좌우 4칸에 몹이 모이면 중심 몹을 마우스로 찍어 삼매진화를 쓴다 (삼매진화는 대상과 상하좌우를 친다).
 *
 * 각을 만드는 순서 (한 번에 하나씩, 탐지 결과를 보고 고른다)
 * 1. 상하좌우가 다 찼으면 중심에 삼매진화
 * 2. 상하좌우에 몹이 가장 많은 몹을 중심으로 잡는다 ([MIN_NEIGHBORS]마리 이상일 때만)
 * 3. 중심의 상하좌우에 붙은 몹 중 아직 안 묶은 몹을 절망으로 묶는다. 중심은 안 묶는다 (클릭 하나 아끼기, 사냥 영상 방식)
 * 4. 빈 칸 근처([RELEASE_RANGE]칸 안)에 묶여 있는 몹(절망, 마비 뿌리기로 건 마비)은 활력으로 풀어 빈 칸으로 오게 한다
 * 5. 그래도 빈 칸이 있으면 빈 칸에 가장 가까운 안 묶인 몹에 혼돈을 건다 (다른 몹 쪽으로 오게)
 *
 * 몹이 묶였는지는 화면에 안 보이므로 "어느 칸 몹에 언제 무엇을 걸었는지"를 직접 기억한다.
 * 그 칸에서 몹이 사라지면(죽거나 풀려서 움직임) 기록을 지운다.
 */
class FiveCrossUseCase(
    /** 마비 뿌리기로 마비를 건 칸 */
    private val paralyzed: () -> Set<Tile> = { emptySet() },
    private val detection: (now: Long) -> Detection? = { DetectionStateHolder.state.value?.takeIf { d -> d.isFresh(it) } },
    private val ocr: OcrStateHolder = OcrStateHolder,
    private val now: () -> Long = System::currentTimeMillis,
) : MacroUseCase {
    override val name = "5매각"
    private val reason = ReasonLog("FiveCrossUseCase")

    /** 내가 묶은 칸과 묶은 시각 */
    private val held = mutableMapOf<Tile, Long>()
    private var lastSammeAt: Long? = null
    private var lastHondonAt = 0L
    private var lastReleaseAt = 0L
    private var next: Action? = null

    /** 지금 만들고 있는 각의 칸들 (중심 + 붙은 몹). 마비 같은 다른 기능이 건드리지 않게 알려준다 */
    @Volatile var reserved: Set<Tile> = emptySet()
        private set

    sealed interface Action {
        val aim: Aim
        data class Samme(override val aim: Aim, val center: Tile) : Action
        data class Hold(override val aim: Aim, val tile: Tile) : Action
        data class Release(override val aim: Aim, val tile: Tile) : Action
        data class Chaos(override val aim: Aim) : Action
    }

    /** 내가 [step]만큼 움직이면 기억한 칸도 같이 옮긴다 (칸은 나 기준이라) */
    fun onMoved(step: Tile) {
        val moved = held.mapKeys { it.key.afterMyMove(step) }
        held.clear()
        held.putAll(moved)
        reserved = reserved.map { it.afterMyMove(step) }.toSet()
    }

    override fun isReady(now: Long): Boolean {
        next = plan(now)
        return next != null
    }

    override suspend fun execute() {
        val action = next ?: return
        next = null
        val target = Target.Click(action.aim)
        when (action) {
            is Action.Samme -> if (SkillCaster.tryCast(Skill.SAMME, target)) {
                lastSammeAt = now()
                held.clear()
            }
            is Action.Hold -> if (SkillCaster.tryCast(Skill.JULMANG, target)) held[action.tile] = now()
            is Action.Release -> if (SkillCaster.tryCast(Skill.HWALRYEOK, target)) {
                held.remove(action.tile)
                lastReleaseAt = now()
            }
            is Action.Chaos -> if (SkillCaster.tryCast(Skill.HONDON, target)) lastHondonAt = now()
        }
    }

    /** 지금 할 일 하나. 테스트에서 바로 부를 수 있게 상태만 보고 정한다 */
    fun plan(now: Long): Action? {
        reserved = emptySet()
        val detection = detection(now) ?: return null
        val grid = TileGrid.of(detection) ?: return null
        // 내 칸에 잡힌 몹은 나를 잘못 잡은 것이라 뺀다
        val monsters = detection.monsters.associateBy { grid.tileOf(it) }.filterKeys { it != ME }
        // 몹이 떠난 칸의 기록은 지우고, 오래된 기록은 풀렸다고 본다
        held.entries.removeIf { (tile, at) -> tile !in monsters || now - at > HOLD_MILLIS }

        fun aim(tile: Tile) = Aim(detection.window, monsters.getValue(tile).center())

        val center = monsters.keys
            .filter { it != ME }
            // 붙은 몹이 가장 많은 곳, 같으면 나와 가까운 곳
            .maxWithOrNull(compareBy<Tile> { tile -> tile.neighbors.count { it in monsters } }
                .thenBy { -(it.x * it.x + it.y * it.y) })
            ?: return null
        val filled = center.neighbors.filter { it in monsters }
        reserved = if (filled.size >= MIN_NEIGHBORS) (listOf(center) + center.neighbors).toSet() else emptySet()
        if (filled.size < MIN_NEIGHBORS) {
            reason.log("각 후보 없음 (가장 많이 붙은 곳 ${filled.size}마리)")
            return null
        }

        if (filled.size == 4) {
            if (!sammeReady(now)) return null
            reason.log("5매각 완성 -> 삼매진화 $center")
            return Action.Samme(aim(center), center)
        }

        val cross = listOf(center) + filled
        filled.firstOrNull { it !in held }?.let {
            reason.log("각 ${filled.size + 1}마리, $it 묶기")
            return Action.Hold(aim(it), it)
        }

        val empty = center.neighbors.filter { it !in monsters && it != ME }
        fun nearEmpty(tile: Tile) = empty.any { e -> (e.x - tile.x) * (e.x - tile.x) + (e.y - tile.y) * (e.y - tile.y) <= RELEASE_RANGE * RELEASE_RANGE }
        if (now - lastReleaseAt >= RELEASE_INTERVAL_MILLIS) {
            val stuck = held.keys.filter { now - held.getValue(it) >= RELEASE_AFTER_MILLIS } + paralyzed()
            stuck.firstOrNull { it in monsters && it !in cross && nearEmpty(it) }?.let {
                reason.log("각 밖에 묶인 몹 $it 풀기")
                return Action.Release(aim(it), it)
            }
        }

        if (now - lastHondonAt >= HONDON_INTERVAL_MILLIS) {
            val free = monsters.keys.filter { it !in held && it !in cross }
            val pick = free.minByOrNull { tile -> empty.minOf { (it.x - tile.x) * (it.x - tile.x) + (it.y - tile.y) * (it.y - tile.y) } }
            if (pick != null && empty.isNotEmpty()) {
                reason.log("빈 칸 ${empty.size}개, $pick 에 혼돈")
                return Action.Chaos(aim(pick))
            }
        }
        return null
    }

    private fun sammeReady(now: Long): Boolean {
        lastSammeAt?.let { if (now - it < SammeUseCase.RECAST_GUARD_MILLIS) return false }
        val cooldown = ocr.state.value.fresh(TimerRegion.COOLDOWN, now) ?: run {
            reason.log("쿨타임 박스 읽기 실패라 삼매 안 씀")
            return false
        }
        val remaining = cooldown.find(SammeUseCase.NAME)?.remainingSeconds(now)
        if (remaining != null && remaining > 0) {
            reason.log("5매각 완성, 삼매 쿨 $remaining 초")
            return false
        }
        return true
    }

    companion object {
        private val ME = Tile(0, 0)
        /** 중심 몹 상하좌우에 이만큼은 붙어 있어야 각을 만들기 시작한다 */
        const val MIN_NEIGHBORS = 2
        /** 절망이 풀렸다고 보는 시간. 실제 지속시간을 알면 맞춘다 */
        const val HOLD_MILLIS = 10_000L
        /** 묶은 지 이만큼 지났는데 각 밖에 있으면 활력으로 푼다 */
        const val RELEASE_AFTER_MILLIS = 2_000L
        const val RELEASE_INTERVAL_MILLIS = 1_000L
        /** 빈 칸에서 이 칸 수 안에 묶인 몹만 풀어 준다 (멀리 있는 몹은 풀어도 각으로 안 온다) */
        const val RELEASE_RANGE = 2
        /** 혼돈 지속시간(5초) 동안은 다시 걸지 않는다 */
        const val HONDON_INTERVAL_MILLIS = 5_000L
    }
}
