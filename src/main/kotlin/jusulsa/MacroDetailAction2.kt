package jusulsa

import common.robot.Keyboard
import common.robot.UserInput
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.SkillCaster.cast
import jusulsa.skill.Target
import jusulsa.usecase.BomuUseCase
import jusulsa.usecase.HealUseCase
import jusulsa.usecase.HellfireUseCase
import jusulsa.usecase.MagiUseCase
import jusulsa.usecase.ManaUseCase
import jusulsa.usecase.SammeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.event.KeyEvent

/**
 * 주술사 매크로. 무엇을 어떤 순서로 쓸지만 정하고,
 * 초당 횟수, 최소 간격, 사용자 이동키 같은 규칙은 SkillCaster가 지킨다.
 */
class MacroDetailAction2(
    private val bomu: BomuUseCase = BomuUseCase(),
    private val magi: MagiUseCase = MagiUseCase(),
    private val selfHeal: HealUseCase = HealUseCase(),
    private val mana: ManaUseCase = ManaUseCase(),
    private val sammeUseCase: SammeUseCase = SammeUseCase(),
    private val hellfire: HellfireUseCase = HellfireUseCase(),
) {
    private var latestDirection: Int = KeyEvent.VK_LEFT

    fun onDirectionChanged(event: Int) {
        latestDirection = event
    }

    suspend fun hellfire() = Keyboard.atomic {
        cast(Skill.JEOJU, Target.Direction(latestDirection, fromMe = true))
        cast(Skill.HELLFIRE, Target.Confirm)
    }

    suspend fun heal() {
        cast(Skill.HEAL, Target.Me)
        repeat(2) { cast(Skill.HEAL) }

        bomu()

        tabTab()
        while (true) {
            currentCoroutineContext().ensureActive()
            // 보무를 걸면 대상이 나로 바뀌므로 다시 탭탭으로 잡는다
            if (bomu()) tabTab()
            // 초당 3번은 SkillCaster가 맞춘다
            cast(Skill.HEAL)
        }
    }


    suspend fun jeoju() {
        tabTab()
        while (true) {
            currentCoroutineContext().ensureActive()
            cast(Skill.JEOJU)
        }
    }



    suspend fun maagi() {
        magi.cast()
    }

    suspend fun samme() {
        cast(Skill.SAMME)
    }

    suspend fun gongjeung() {
        cast(Skill.GONGJEUNG)
    }

    suspend fun hondon() {
        cast(Skill.HONDON)
    }

    /**
     * 첨첨. 키 입력은 한 번에 하나뿐이라 여러 루프가 락을 두고 다투지 않게 한 루프에서 우선순위대로 하나씩 고른다.
     * 고른 마법은 기다리지 않고 바로 쓸 수 있는 것만 쓰고, 아무것도 못 쓰면 잠깐 쉬었다 다시 고른다.
     *
     * 1. 사용자가 이동 중이면 아무것도 안 쓴다
     * 2. 체력이 낮으면 자힐, 마력이 낮으면 공증
     * 3. 보무, 마기지체, 삼매진화가 필요하면
     * 4. 극진뢰·진뢰
     * 5. 남는 시간에 저주+중독을 한 방향씩 멀리 퍼뜨린다
     */
    suspend fun chumChum() = kotlinx.coroutines.coroutineScope {
        val attack = CursePoisonCycle(hellfire)

        // 1. 공격 전담 코루틴: 첨첨 마법 2개는 최우선으로 끊임없이 발사 (이동 중이 아닐 때만)
        launch(Dispatchers.Default) {
            while (isActive) {
                val moving = UserInput.waitMillis()
                if (moving > 0) {
                    delay(moving)
                    continue
                }
                
                if (!chum()) {
                    delay(IDLE_MILLIS)
                }
            }
        }

        // 2. 보조 전담 코루틴: 힐, 저주/중독, 삼매, 버프 등은 선택적으로 돌아감
        launch(Dispatchers.Default) {
            while (isActive) {
                val moving = UserInput.waitMillis()
                if (moving > 0) {
                    delay(moving)
                    continue
                }

                // || (OR) 로 묶여 있어서 앞에서 하나라도 실행되면 뒤의 것은 스킵됨 (선택적 실행)
                // 마력이 없으면 힐도 안 나가므로 공증이 가장 먼저다. 삼매는 마력을 다 쓰므로 쓰자마자 공증한다
                val casted = mana() || selfHeal() ||
                    bomu() || magi() || sammeThenGongjeung() ||
                    attack.step()
                
                if (!casted) {
                    delay(IDLE_MILLIS)
                } else {
                    // 보조 마법을 하나 걸었으면, 공격(첨첨) 코루틴이 키보드를 쓸 수 있도록 잠깐 숨을 고름
                    delay(60L)
                }
            }
        }
    }

    /** 삼매진화를 쓰면 마력이 0이 되므로, 막대를 다시 읽을 때까지 기다리지 않고 바로 공증한다 */
    private suspend fun sammeThenGongjeung(): Boolean {
        if (!sammeUseCase()) return false
        mana.afterManaSpent()
        return true
    }

    private suspend fun chum(): Boolean {
        val skill = CHUMS.firstOrNull { SkillCaster.readyIn(it) == 0L } ?: return false
        cast(skill)
        return true
    }

    /**
     * 한 방향으로 저주를 걸어 다음 몹으로 커서를 옮기고, 같은 몹에 바로 중독을 건다. 헬파이어가 쿨이 아니면 그 몹에 같이 쓴다.
     * 방향키는 지금 커서 위치에서 그 방향의 다음 몹으로 옮겨 가므로, 같은 방향을 이어서 눌러야 멀리까지 퍼진다.
     * 한 방향에 [CURSE_PER_DIRECTION]마리를 걸고 다음 방향으로 넘어간다.
     * 힐·삼매·보무를 쓰면 커서가 나로 돌아오므로 그 뒤에는 내 주변부터 다시 퍼진다.
     */
    private class CursePoisonCycle(private val hellfire: HellfireUseCase) {
        private var count = 0

        /** 지금 쓸 수 있으면 저주+중독 한 쌍을 쓴다. 썼으면 true */
        suspend fun step(): Boolean {
            if (SkillCaster.readyIn(Skill.JEOJU) > 0) return false
            val dir = DIRECTIONS[(count / CURSE_PER_DIRECTION) % DIRECTIONS.size]

            // 둘 사이에 다른 마법이 끼면 중독이 엉뚱한 대상(나)에게 갈 수 있어서 한 번에 보낸다
            val casted = Keyboard.atomic {
                if (!SkillCaster.tryCast(Skill.JEOJU, Target.Direction(dir))) return@atomic false
                if (hellfire.canCast() && SkillCaster.tryCast(Skill.HELLFIRE, Target.Confirm)) hellfire.onCast()
                SkillCaster.tryCast(Skill.JUNGDOK)
                true
            }
            if (casted) count++
            return casted
        }
    }

    suspend fun tabTab() = Keyboard.atomic {
        val duration = 30L
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, duration)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME, duration)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, duration)
    }

    companion object {
        /** 쓸 수 있는 마법이 없을 때 다시 고르기까지 쉬는 시간 */
        private const val IDLE_MILLIS = 20L
        private val CHUMS = listOf(Skill.CHUM1, Skill.CHUM2)
        /** 한 방향에 몇 마리를 걸고 다음 방향으로 넘어갈지 */
        private const val CURSE_PER_DIRECTION = 5
        private val DIRECTIONS = listOf(
            KeyEvent.VK_UP,
            KeyEvent.VK_LEFT,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_RIGHT
        )
    }
}
