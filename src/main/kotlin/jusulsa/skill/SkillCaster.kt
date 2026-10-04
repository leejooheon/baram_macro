package jusulsa.skill

import common.robot.Keyboard
import common.robot.UserInput
import kotlinx.coroutines.delay
import java.awt.event.KeyEvent
import java.util.concurrent.ConcurrentHashMap

/**
 * 모든 마법 시전이 거치는 곳. 매크로와 UseCase는 무엇을 쓸지만 정하고, 언제 써도 되는지는 여기서 지킨다.
 *
 * - 초당 횟수: [RateGroup]별로 직전 1초 안에 limit번까지만 (넘으면 게임이 무시하므로 여유는 두지 않는다)
 * - 최소 간격: [Skill.minIntervalMillis]
 * - 사용자 이동키: 사용자가 방향키로 이동 중이면 어떤 마법도 쓰지 않고 기다린다.
 *   방향으로 대상을 잡는 도중에 사용자 방향키가 들어오면 ESC로 취소하고 다시 시도한다.
 */
object SkillCaster {
    const val SKILL_DELAY = 60L

    private val limiters = RateGroup.entries.associateWith { RateLimiter(it.limit) }
    private val lastCastAt = ConcurrentHashMap<Skill, Long>()

    /** 규칙을 지킬 수 있을 때까지 기다렸다가 시전한다 */
    suspend fun cast(skill: Skill, target: Target = Target.Current) {
        while (true) {
            val wait = waitMillis(skill, now())
            if (wait > 0) {
                delay(wait)
                continue
            }
            // 기다리는 사이 다른 매크로가 먼저 썼을 수 있으니 입력 락 안에서 다시 확인한다
            if (Keyboard.atomic { tryCast(skill, target) }) return
        }
    }

    suspend fun tryCast(skill: Skill, target: Target = Target.Current): Boolean {
        val startedAt = now()
        if (waitMillis(skill, startedAt) > 0) return false

        select(skill)
        when (target) {
            Target.Current -> Unit
            Target.Confirm -> confirm(skill)
            Target.Me -> {
                pause(skill)
                Keyboard.pressAndRelease(KeyEvent.VK_HOME)
                confirm(skill)
            }
            is Target.Direction -> {
                if (target.fromMe) {
                    pause(skill)
                    Keyboard.pressAndRelease(KeyEvent.VK_HOME)
                }
                pause(skill)
                Keyboard.pressAndRelease(target.direction)
                if (UserInput.lastArrowAt >= startedAt || UserInput.isMoving()) {
                    Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
                    return false
                }
                confirm(skill)
            }
        }

        // 마지막 키를 보낸 시점을 시전 시각으로 센다. 시작할 때 확인했고 그 사이엔 락을 잡고 있었으므로 제한을 넘지 않는다
        val castAt = now()
        skill.rateGroup?.let { limiters.getValue(it).record(castAt) }
        lastCastAt[skill] = castAt
        return true
    }

    /** 지금 바로 쓸 수 있으면 0, 아니면 기다려야 할 시간(ms). 매크로가 기다리지 않고 다른 마법을 고를 때 쓴다 */
    fun readyIn(skill: Skill): Long = waitMillis(skill, now())

    /** 이 묶음을 지금 바로 몇 번 더 쓸 수 있는지 */
    fun remaining(group: RateGroup): Int = limiters.getValue(group).remaining(now())

    private fun waitMillis(skill: Skill, now: Long): Long {
        var wait = 0L
        skill.rateGroup?.let { wait = maxOf(wait, limiters.getValue(it).waitMillis(now)) }
        if (skill.minIntervalMillis > 0) {
            lastCastAt[skill]?.let { wait = maxOf(wait, it + skill.minIntervalMillis - now) }
        }
        return maxOf(wait, UserInput.waitMillis(now))
    }

    private suspend fun select(skill: Skill) {
        if (skill.alphabet) selectAlphabet(skill.key, skill.upper)
        else Keyboard.pressAndRelease(skill.key)
    }

    private suspend fun confirm(skill: Skill) {
        pause(skill)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
    }

    // 알파벳 마법은 선택창이 뜨는 시간이 있어서 다음 키 전에 조금 쉰다
    private suspend fun pause(skill: Skill) {
        if (skill.alphabet) delay(SKILL_DELAY)
    }

    // shift+z 후 알파벳, 대문자 칸은 shift를 누른 채로 알파벳까지 입력
    private suspend fun selectAlphabet(key: Int, upper: Boolean) {
        Keyboard.press(KeyEvent.VK_SHIFT)
        try {
            delay(SKILL_DELAY)
            Keyboard.pressAndRelease(KeyEvent.VK_Z)
            if (upper) {
                delay(SKILL_DELAY)
                Keyboard.pressAndRelease(key)
            }
        } finally {
            Keyboard.release(KeyEvent.VK_SHIFT)
        }

        if (!upper) {
            delay(SKILL_DELAY)
            Keyboard.pressAndRelease(key)
        }
    }

    private fun now() = System.currentTimeMillis()
}
