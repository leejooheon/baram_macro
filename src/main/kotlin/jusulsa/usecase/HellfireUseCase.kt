package jusulsa.usecase

import common.robot.Keyboard
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.Target
import ocr.OcrStateHolder
import ocr.model.TimerRegion

class HellfireUseCase(
    private val ocr: OcrStateHolder = OcrStateHolder,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var lastCastAt: Long? = null
    var latestDirection: Int = java.awt.event.KeyEvent.VK_LEFT

    fun canCast(): Boolean {
        val time = now()
        
        lastCastAt?.let { 
            if (time - it < RECAST_GUARD_MILLIS) return false 
        }

        val cooldown = ocr.state.value.fresh(TimerRegion.COOLDOWN, time)
        if (cooldown != null) {
            val remaining = cooldown.find(NAME)?.remainingSeconds(time) ?: 0
            if (remaining > 0) return false
        }
        
        return true
    }

    suspend operator fun invoke(): Boolean {
        if (!canCast()) return false
        
        var hellfireCasted = false
        Keyboard.atomic {
            if (SkillCaster.tryCast(Skill.HELLFIRE, Target.Direction(latestDirection, fromMe = true))) {
                hellfireCasted = true
            }
        }
        
        if (hellfireCasted) {
            lastCastAt = now()
            return true
        }
        return false
    }

    companion object {
        const val NAME = "헬파이어"
        const val RECAST_GUARD_MILLIS = 1_000L
    }
}
