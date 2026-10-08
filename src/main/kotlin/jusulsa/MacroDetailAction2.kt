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
     * 첨첨. 게임은 키를 누르고 있으면 알아서 계속 시전하므로, 매크로는 켜 두기만 한다.
     *
     * - 첨(3, 4): 시작할 때 3·4를 누른 채 넘버락을 누르면 그 뒤로는 키를 안 눌러도 계속 나간다. 멈출 때 넘버락을 한 번 더 누른다
     * - 평타: 스페이스 140ms 간격
     * - 보조: 공증 > 헬파 > 삼매 > 보무 > 마기 > 중독(7초마다) > 저주(10초마다)
     */
    suspend fun chumChum() = kotlinx.coroutines.coroutineScope {
        startAutoChum()
        try {
            chumChumLoops()
        } finally {
            withContext(NonCancellable) { Keyboard.pressAndRelease(KeyEvent.VK_NUM_LOCK) }
        }
    }

    private suspend fun chumChumLoops() = kotlinx.coroutines.coroutineScope {
        var lastJeojuTime = 0L
        var lastJungdokTime = 0L
        var jungdokDirection = 0

        // 1. 평타 전담 코루틴: 140ms 간격으로 스페이스바 연타 (예전 코드 템포 그대로 복구)
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

        // 2. 보조 전담 코루틴: 공증, 헬파, 삼매, 보무, 마기, 중독, 저주
        launch(Dispatchers.Default) {
            while (isActive) {
                val moving = UserInput.waitMillis()
                if (moving > 0) {
                    delay(moving)
                    continue
                }

                val now = System.currentTimeMillis()
                val casted = mana() || hellfireUseCase() || sammeUseCase() || bomu() || magi() ||
                    (now - lastJungdokTime >= JUNGDOK_MILLIS && run {
                        holdCast(Skill.JUNGDOK, DIRECTIONS[jungdokDirection])
                        jungdokDirection = (jungdokDirection + 1) % DIRECTIONS.size
                        lastJungdokTime = System.currentTimeMillis()
                        true
                    }) ||
                    (now - lastJeojuTime >= JEOJU_MILLIS && run {
                        holdCast(Skill.JEOJU)
                        lastJeojuTime = System.currentTimeMillis()
                        true
                    })

                if (!casted) {
                    delay(IDLE_MILLIS)
                } else {
                    // 보조 마법을 하나 시전했으면, 평타가 틈을 탈 수 있게 잠깐 딜레이
                    delay(60L)
                }
            }
        }
    }

    /** 3·4를 누른 채 넘버락을 누르면 게임이 첨을 알아서 계속 쓴다. 그 뒤엔 3·4를 떼도 된다 */
    private suspend fun startAutoChum() = Keyboard.atomic {
        val chums = listOf(Skill.CHUM1.key, Skill.CHUM2.key)
        try {
            chums.forEach {
                Keyboard.press(it)
                delay(Keyboard.DEFAULT_DELAY)
            }
            Keyboard.pressAndRelease(KeyEvent.VK_NUM_LOCK)
        } finally {
            withContext(NonCancellable) { chums.reversed().forEach { Keyboard.release(it) } }
        }
    }

    /**
     * 마법 칸 키를 누른 채로 [HOLD_MILLIS] 동안 두면 게임이 알아서 계속 시전한다.
     * direction을 주면 칸 키 -> 방향키 -> 엔터를 차례로 누른 채로 둔다 (중독, 절망).
     * direction이 없으면 칸 키만 누른 채로 둔다 (저주).
     * 절망 같은 다른 마법도 칸만 바꿔서 그대로 쓴다.
     */
    suspend fun holdCast(skill: Skill, direction: Int? = null) = Keyboard.atomic {
        val keys = buildList {
            add(skill.key)
            if (direction != null) {
                add(direction)
                add(KeyEvent.VK_ENTER)
            }
        }
        val pressed = mutableListOf<Int>()
        try {
            keys.forEach {
                Keyboard.press(it)
                pressed.add(it)
                delay(Keyboard.DEFAULT_DELAY)
            }
            // 프로그램이 누른 키는 눌러만 둬서는 반복 입력이 안 생기므로, 실제 키보드처럼 마지막 키를 계속 다시 누른다
            val until = System.currentTimeMillis() + HOLD_MILLIS
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
        private const val JEOJU_MILLIS = 10_000L
        private const val JUNGDOK_MILLIS = 7_000L
        /** 저주·중독 키를 누르고 있는 시간 */
        private const val HOLD_MILLIS = 1_500L
        /** 누르고 있는 동안 키를 다시 보내는 간격 (실제 키보드의 반복 입력 대신) */
        private const val REPEAT_MILLIS = 30L
        /** 중독을 걸 방향. 한 번에 한 방향씩 돌아가며 쓴다 */
        private val DIRECTIONS = listOf(
            KeyEvent.VK_UP,
            KeyEvent.VK_LEFT,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_RIGHT
        )
    }
}
