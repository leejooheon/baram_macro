package jusulsa

import common.robot.Keyboard
import common.robot.UserInput
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster.cast
import jusulsa.skill.Target
import jusulsa.usecase.BomuUseCase
import jusulsa.usecase.HealUseCase
import jusulsa.usecase.HellfireUseCase
import jusulsa.usecase.MagiUseCase
import jusulsa.usecase.ManaUseCase
import jusulsa.usecase.SammeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
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
    private val hellfireUseCase: HellfireUseCase = HellfireUseCase(),
) {
    private var latestDirection: Int = KeyEvent.VK_LEFT

    fun onDirectionChanged(event: Int) {
        latestDirection = event
        hellfireUseCase.latestDirection = event
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
        var lastJeojuTime = 0L
        val jeojuQueue = ArrayDeque<Int>()

        var lastJungdokTime = 0L
        // 1-1. 평타 전담 코루틴: 140ms 간격으로 스페이스바 연타 (예전 코드 템포 그대로 복구)
        launch(Dispatchers.Default) {
            while (isActive) {
                val moving = UserInput.waitMillis()
                if (moving > 0) {
                    delay(moving)
                    continue
                }
                // 아주 짧은 락(10ms)만 걸고 바로 빠짐
                Keyboard.pressAndRelease(KeyEvent.VK_SPACE, 10L)
                delay(140)
            }
        }
        
        // 1-2. 첨첨 전담 코루틴: 3번, 4번 마법을 400ms, 520ms 간격으로 교차 시전 (예전 코드 템포 그대로 복구)
        launch(Dispatchers.Default) {
            while (isActive) {
                val moving = UserInput.waitMillis()
                if (moving > 0) {
                    delay(moving)
                    continue
                }
                
                // 복잡한 SkillCaster 판정을 거치지 않고 다이렉트로 키를 꽂음
                Keyboard.pressAndRelease(Skill.CHUM1.key, 10L)
                delay(400)
                
                Keyboard.pressAndRelease(Skill.CHUM2.key, 10L)
                delay(520)
            }
        }
        
        // 2. 보조 전담 코루틴: 공증, 헬파, 삼매, 보무, 마기, 저주, 중독
        launch(Dispatchers.Default) {
            while (isActive) {
                val moving = UserInput.waitMillis()
                if (moving > 0) {
                    delay(moving)
                    continue
                }


                val now = System.currentTimeMillis()
                
                // 7초가 지났고 큐가 비어있다면 4방향 대기열에 추가
                if (now - lastJeojuTime >= 7000L && jeojuQueue.isEmpty()) {
                    jeojuQueue.addAll(DIRECTIONS)
                    lastJeojuTime = now
                }

                // 우선순위: 공증 -> 헬파 -> 삼매 -> 보무 -> 마기 -> 저주(큐에 남은 것 1개씩 처리)
                val casted = mana() || hellfireUseCase() || sammeUseCase() || bomu() || magi() ||
                    (jeojuQueue.isNotEmpty() && run {
                        holdCast(Skill.JEOJU, jeojuQueue.removeFirst())
                        true
                    })
                
                if (!casted) {
                    delay(IDLE_MILLIS)
                } else {
                    // 보조 마법을 하나 시전했으면, 공격 코루틴이 틈을 탈 수 있게 잠깐 딜레이
                    delay(60L)
                }
            }
        }
    }

    /**
     * 마법 칸 키 -> 방향키 -> 엔터를 차례로 누른 채 [HOLD_MILLIS] 동안 두면 게임이 그 방향으로 계속 시전한다.
     * 저주, 중독, 절망이 같이 쓴다. 다른 마법은 칸만 바꿔 넘기면 된다.
     */
    suspend fun holdCast(skill: Skill, direction: Int, holdMillis: Long = HOLD_MILLIS) = Keyboard.atomic {
        val keys = listOf(skill.key, direction, KeyEvent.VK_ENTER)
        val pressed = mutableListOf<Int>()
        try {
            keys.forEach {
                Keyboard.press(it)
                pressed.add(it)
                delay(Keyboard.DEFAULT_DELAY)
            }
            // 프로그램이 누른 키는 눌러만 둬서는 반복 입력이 안 생기므로, 실제 키보드처럼 마지막 키(엔터)를 계속 다시 누른다
            val until = System.currentTimeMillis() + holdMillis
            while (System.currentTimeMillis() < until) {
                delay(REPEAT_MILLIS)
                Keyboard.press(keys.last())
            }
        } finally {
            withContext(NonCancellable) { pressed.reversed().forEach { Keyboard.release(it) } }
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
        /** 저주·중독·절망 키를 누르고 있는 시간 (한 방향) */
        private const val HOLD_MILLIS = 1_000L
        /** 누르고 있는 동안 키를 다시 보내는 간격 (실제 키보드의 반복 입력 대신) */
        private const val REPEAT_MILLIS = 30L
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
