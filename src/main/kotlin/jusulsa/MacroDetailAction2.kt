package jusulsa

import common.robot.Keyboard
import common.robot.UserInput
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster
import jusulsa.skill.SkillCaster.cast
import jusulsa.skill.Target
import jusulsa.usecase.BomuUseCase
import jusulsa.usecase.HealUseCase
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
     * 4. 극진뢰·진뢰 (각각 400ms 간격)
     * 5. 남는 시간에 저주 5초 -> 4방향 중독 30초 반복
     */
    suspend fun chumChum() = kotlinx.coroutines.coroutineScope {
        val attack = CursePoisonCycle()

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
                // 힐이나 마력 충전이 급하면 먼저 하고, 여유 있을 때 삼매나 저주/중독을 건다
                val casted = selfHeal() || mana() ||
                    bomu() || magi() || sammeUseCase() ||
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

    private suspend fun chum(): Boolean {
        val skill = CHUMS.firstOrNull { SkillCaster.readyIn(it) == 0L } ?: return false
        cast(skill)
        return true
    }

    /** 저주와 중독을 0.5초 간격으로 번갈아가며 4방향으로 골고루 건다 */
    private class CursePoisonCycle {
        private var lastCastAt = 0L
        private val interval = 500L // 0.5초 간격으로 하나씩 시전
        private var isJeojuTurn = true
        private var directionIndex = 0

        /** 지금 쓸 수 있으면 하나 쓴다. 썼으면 true */
        suspend fun step(): Boolean {
            val now = System.currentTimeMillis()
            if (now - lastCastAt < interval) return false

            val dir = DIRECTIONS[directionIndex]
            var casted = false
            
            if (isJeojuTurn) {
                if (SkillCaster.readyIn(Skill.JEOJU) == 0L) {
                    if (Keyboard.atomic { SkillCaster.tryCast(Skill.JEOJU, Target.Direction(dir)) }) {
                        casted = true
                        isJeojuTurn = false // 다음엔 중독
                        lastCastAt = now
                    }
                }
            } else {
                if (SkillCaster.readyIn(Skill.JUNGDOK) == 0L) {
                    if (Keyboard.atomic { SkillCaster.tryCast(Skill.JUNGDOK, Target.Direction(dir)) }) {
                        casted = true
                        isJeojuTurn = true // 다음엔 저주
                        directionIndex = (directionIndex + 1) % DIRECTIONS.size // 중독까지 걸었으면 다음 방향으로 회전
                        lastCastAt = now
                    }
                }
            }
            
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
        private const val JEOJU_MILLIS = 5_000L
        private const val JUNGDOK_MILLIS = 30_000L
        private val CHUMS = listOf(Skill.CHUM1, Skill.CHUM2)
        /** 한 방향에 중독을 몇 번 걸고 다음 방향으로 넘어갈지 */
        private const val JUNGDOK_PER_DIRECTION = 9
        private val DIRECTIONS = listOf(
            KeyEvent.VK_UP,
            KeyEvent.VK_LEFT,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_RIGHT
        )
    }
}
