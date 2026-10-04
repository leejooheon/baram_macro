package jusulsa

import common.robot.Keyboard
import jusulsa.skill.Skill
import jusulsa.skill.SkillCaster.cast
import jusulsa.skill.Target
import jusulsa.engine.MacroEngine
import jusulsa.usecase.BasicAttackUseCase
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
     * 첨첨. 할 일은 전부 UseCase이고, [MacroEngine]이 상태를 보고 하나씩 골라 실행한다.
     *
     * 우선(앞에서부터, 할 일이 있으면 바로): 공증 > 자힐 > 보무 > 마기지체 > 삼매진화 > 저주(사방) > 헬파이어
     * 공격(번갈아 가며): 첨 > 6번 맵 전체 > 평타
     * 사용자가 방향키로 이동 중이면 아무것도 안 한다.
     */
    suspend fun chumChum() = withContext(Dispatchers.Default) {
        MacroEngine(
            priority = listOf(mana, selfHeal, bomu, magi, sammeUseCase, CurseAroundUseCase(), HellfireUseCase({ latestDirection })),
            rotation = listOf(ChumUseCase(), DespairSpreadUseCase({ latestDirection }), BasicAttackUseCase()),
        ).run()
    }

    suspend fun tabTab() = Keyboard.atomic {
        val duration = 30L
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, duration)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME, duration)
        Keyboard.pressAndRelease(KeyEvent.VK_TAB, duration)
    }

}
