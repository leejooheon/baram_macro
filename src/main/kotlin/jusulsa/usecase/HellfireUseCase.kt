package jusulsa.usecase

import jusulsa.engine.MacroUseCase
import jusulsa.engine.ReasonLog
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.Target
import ocr.OcrStateHolder
import ocr.model.TimerRegion

/**
 * 헬파이어. 쿨타임 박스에 헬파이어가 없으면 쓴다. 삼매진화가 [SAMME_HOLD_SECONDS]초 안에 돌아오면 기다린다.
 * 예전 헬파이어 단축키와 같이, 나를 기준으로 [direction] 방향 몹에 저주를 걸어 대상을 잡은 뒤 1 + Enter로 쓴다.
 *
 * 마력이 공증 기준 이하면 쓰지 않는다. 마력이 없어서 1이 안 먹으면 뒤의 Enter가 채팅창을 열고,
 * 그 뒤에 보내는 키가 전부 채팅으로 들어가 매크로가 먹통이 된다.
 * 헬파이어로 마력이 바닥나면 공증 UseCase가 상태를 보고 채운다.
 */
class HellfireUseCase(
    private val direction: () -> Int,
    private val ocr: OcrStateHolder = OcrStateHolder,
    private val now: () -> Long = System::currentTimeMillis,
) : MacroUseCase {
    override val name = "헬파이어"
    private var lastCastAt: Long? = null
    private val reason = ReasonLog("HellfireUseCase")

    override fun isReady(now: Long): Boolean {
        // 쓴 직후에는 쿨타임 박스에 아직 안 잡힌다. 정해 둔 시간 대신, 쓴 뒤에 찍은 쿨타임 박스가 들어올 때까지만 기다린다
        lastCastAt?.let { if (now - it < MIN_GUARD_MILLIS) return false }

        if (!ManaReading.isEnough(ocr, now, reason::log)) return false
        val cooldown = ocr.state.value.fresh(TimerRegion.COOLDOWN, now)
        if (cooldown == null) {
            reason.log("쿨타임 박스 읽기 실패라 안 씀")
            return false
        }
        lastCastAt?.let {
            if (cooldown.capturedAt < it + CAPTURE_LAG_MILLIS) {
                reason.log("쓴 뒤 쿨타임 박스 다시 읽는 중")
                return false
            }
        }
        // 삼매진화가 곧 돌아오면 마력을 아껴 둔다. 삼매가 쓸 수 있는 상태면 우선 목록에서 삼매가 먼저 나간다
        val samme = cooldown.find(SammeUseCase.NAME)?.remainingSeconds(now)
        if (samme != null && samme in 1..SAMME_HOLD_SECONDS) {
            reason.log("삼매진화 쿨 $samme 초 남아서 마력 아껴 둠")
            return false
        }
        // OCR이 이름을 조금씩 다르게 읽어서 "헬"만 보고 찾는다
        val remaining = cooldown.entries.firstOrNull { NAME_KEY in it.name }?.remainingSeconds(now)
        if (remaining != null && remaining > 0) {
            reason.log("쿨타임 $remaining 초 남음")
            return false
        }
        reason.log("쿨 아님 -> 사용")
        return true
    }

    override suspend fun execute() {
        // 저주로 대상을 못 잡았으면(이동키로 취소 등) 1 + Enter를 보내지 않는다
        if (!SkillCaster.tryCast(Skill.JEOJU, Target.Direction(direction(), fromMe = true))) return
        if (SkillCaster.tryCast(Skill.HELLFIRE, Target.Confirm)) lastCastAt = now()
    }

    companion object {
        const val NAME_KEY = "헬"
        /** 쓴 뒤 최소한 기다리는 시간. 1 + Enter가 연달아 나가 채팅창이 열리는 것을 막는다 */
        const val MIN_GUARD_MILLIS = 1_000L
        /** 쓴 뒤 게임 쿨타임 박스에 헬파이어가 뜨기까지 걸리는 시간 */
        const val CAPTURE_LAG_MILLIS = 300L
        /** 삼매진화 쿨타임이 이 이하로 남았으면 헬파이어를 쓰지 않고 기다린다 */
        const val SAMME_HOLD_SECONDS = 3
    }
}
