package jusulsa.usecase

import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import ocr.OcrStateHolder

/**
 * 마력을 크게 쓰는 마법(헬파이어, 삼매진화, 공증) 뒤에는 막대를 새로 읽기 전까지 마력을 모른다.
 * 쓰기 전 값을 믿으면 헬파이어 직후 마력이 없는데도 삼매진화를 쓰게 되므로, 마지막 사용 이후에 읽은 값만 돌려준다.
 */
object ManaReading {
    /** 마법을 쓴 뒤 게임 막대가 바뀌기까지 기다리는 시간 */
    const val BAR_LAG_MILLIS = 150L
    private val SPENDERS = listOf(Skill.HELLFIRE, Skill.SAMME, Skill.GONGJEUNG)

    sealed interface Result {
        data class Percent(val value: Int) : Result
        /** 막대를 못 읽었거나 오래됨 */
        data object Unknown : Result
        /** 마력을 쓴 뒤 아직 새로 안 읽음 */
        data object Pending : Result
    }

    fun read(ocr: OcrStateHolder, now: Long): Result {
        val vitals = ocr.state.value.freshVitals(now) ?: return Result.Unknown
        val spentAt = SPENDERS.mapNotNull { SkillCaster.lastCastAt(it) }.maxOrNull()
        if (spentAt != null && vitals.capturedAt < spentAt + BAR_LAG_MILLIS) return Result.Pending
        return vitals.mpPercent?.let { Result.Percent(it) } ?: Result.Unknown
    }

    /** 공증 기준보다 마력이 넉넉한지. 못 읽었으면 이유를 [onSkip]으로 알린다 */
    fun isEnough(ocr: OcrStateHolder, now: Long, onSkip: (String) -> Unit): Boolean =
        when (val mp = read(ocr, now)) {
            Result.Unknown -> false.also { onSkip("마력 막대 읽기 실패라 안 씀") }
            Result.Pending -> false.also { onSkip("마력 쓴 뒤 막대 다시 읽는 중") }
            is Result.Percent -> (mp.value > OcrStateHolder.MANA_LOW_PERCENT)
                .also { if (!it) onSkip("마력 ${mp.value}% 라서 안 씀 (공증 기준 이하)") }
        }
}
