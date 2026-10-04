package jusulsa.usecase

import detector.Aim
import detector.DetectionMonitor
import detector.DetectionStateHolder
import jusulsa.engine.MacroUseCase
import jusulsa.engine.ReasonLog
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.Target
import ocr.OcrStateHolder
import ocr.model.TimerRegion

/**
 * 헬파이어. 쿨타임 박스에 헬파이어가 없으면 쓴다.
 * 몹 탐지 결과가 있으면 나와 가장 가까운 몹을 마우스로 찍어 저주를 걸고, 그 대상에게 1 + Enter로 쓴다.
 * 탐지 중인데 화면에 몹이 없으면 쓰지 않는다. 탐지 결과가 없으면(모델 학습 전, 탐지 꺼짐) 예전처럼 나를 기준으로 [direction] 방향 몹에 저주를 건다.
 *
 * 마력이 공증 기준 이하면 쓰지 않는다. 마력이 없어서 1이 안 먹으면 뒤의 Enter가 채팅창을 열고,
 * 그 뒤에 보내는 키가 전부 채팅으로 들어가 매크로가 먹통이 된다.
 * 헬파이어로 마력이 바닥나면 공증 UseCase가 상태를 보고 채운다.
 */
class HellfireUseCase(
    private val direction: () -> Int,
    private val nearestMonster: (now: Long) -> Aim? = DetectionStateHolder::nearestMonster,
    private val detecting: (now: Long) -> Boolean = { DetectionMonitor.state.value.isRunning },
    private val ocr: OcrStateHolder = OcrStateHolder,
    private val now: () -> Long = System::currentTimeMillis,
) : MacroUseCase {
    override val name = "헬파이어"
    private var lastCastAt: Long? = null
    private val reason = ReasonLog("HellfireUseCase")

    override fun isReady(now: Long): Boolean {
        // 쓴 직후에는 쿨타임 박스에 아직 안 잡힌다
        lastCastAt?.let { if (now - it < RECAST_GUARD_MILLIS) return false }
        // 몹 탐지를 켜 뒀는데 (최근 결과에) 몹이 없으면 방향키로 허공에 쏘지 않는다
        if (detecting(now) && nearestMonster(now) == null) return false

        val mp = ocr.state.value.freshVitals(now)?.mpPercent
        if (mp == null || mp <= OcrStateHolder.MANA_LOW_PERCENT) {
            reason.log("마력 ${mp ?: "?"}% 라서 안 씀 (공증 기준 이하)")
            return false
        }
        val cooldown = ocr.state.value.fresh(TimerRegion.COOLDOWN, now)
        if (cooldown == null) {
            reason.log("쿨타임 박스 읽기 실패라 안 씀")
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
        val target = nearestMonster(now())?.let { Target.Click(it) }
            ?: Target.Direction(direction(), fromMe = true)
        // 저주로 대상을 못 잡았으면(이동키로 취소, 클릭 실패 등) 1 + Enter를 보내지 않는다
        if (!SkillCaster.tryCast(Skill.JEOJU, target)) return
        if (SkillCaster.tryCast(Skill.HELLFIRE, Target.Confirm)) lastCastAt = now()
    }

    companion object {
        const val NAME_KEY = "헬"
        const val RECAST_GUARD_MILLIS = 5_000L
    }
}
