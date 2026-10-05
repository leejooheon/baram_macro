package jusulsa

import common.robot.Keyboard
import common.robot.UserInput
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster.cast
import jusulsa.skill.Target
import jusulsa.engine.MacroEngine
import jusulsa.usecase.BomuUseCase
import jusulsa.usecase.ChumUseCase
import jusulsa.usecase.CurseAroundUseCase
import jusulsa.usecase.DespairSpreadUseCase
import jusulsa.usecase.HealUseCase
import jusulsa.usecase.HellfireUseCase
import jusulsa.usecase.MagiUseCase
import jusulsa.usecase.ManaUseCase
import jusulsa.usecase.SammeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    @Volatile
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
     * 첨첨. 예전에 잘 되던 구조(#14)처럼 첨은 따로 도는 루프가 쉬지 않고 쓰고,
     * 나머지는 [MacroEngine]이 상태를 보고 하나씩 고른 뒤 0.06초 쉬어 첨에게 키보드를 넘긴다.
     *
     * 첨 루프: 극진뢰·진뢰를 준비되는 대로 (이동 중이 아닐 때)
     * 엔진 우선: 공증 > 삼매진화 > 헬파이어 > 자힐 > 마기지체 > 보무 > 저주(사방, 5초 쉼)
     * 엔진 그 외: 6번 맵 전체 (10초 쉼)
     * 저주·6번을 몰아 걸 때도 마법 사이에 첨이 준비돼 있으면 끼워 쓴다.
     */
    suspend fun chumChum() = withContext(Dispatchers.Default) {
        val chum = ChumUseCase()
        val chumBetween: suspend () -> Unit = { if (chum.isReady(System.currentTimeMillis())) chum.execute() }

        launch {
            while (isActive) {
                val moving = UserInput.waitMillis()
                if (moving > 0) {
                    delay(moving)
                    continue
                }
                if (chum.isReady(System.currentTimeMillis())) Keyboard.atomic { chum.execute() }
                else delay(CHUM_IDLE_MILLIS)
            }
        }

        MacroEngine(
            priority = listOf(mana, sammeUseCase, HellfireUseCase({ latestDirection }), selfHeal, magi, bomu, CurseAroundUseCase(chumBetween)),
            rotation = listOf(DespairSpreadUseCase({ latestDirection }, chumBetween)),
            breathMillis = AUX_BREATH_MILLIS,
        ).run()
    }

    suspend fun tabTab() = Keyboard.atomic {
        val duration = 30L
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, duration)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME, duration)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, duration)
    }

    companion object {
        /** 첨이 준비 안 됐을 때 다시 보기까지 쉬는 시간 (#14와 같은 값) */
        private const val CHUM_IDLE_MILLIS = 20L
        /** 보조 마법을 하나 쓴 뒤 첨에게 키보드를 넘기려고 쉬는 시간 (#14와 같은 값) */
        private const val AUX_BREATH_MILLIS = 60L
    }
}
