package jusulsa

import jusulsa.engine.MacroEngine
import jusulsa.usecase.BomuUseCase
import jusulsa.usecase.EvadeUseCase
import jusulsa.usecase.FiveCrossUseCase
import jusulsa.usecase.HealUseCase
import jusulsa.usecase.HellfireUseCase
import jusulsa.usecase.MabeeUseCase
import jusulsa.usecase.MagiUseCase
import jusulsa.usecase.ManaUseCase
import jusulsa.usecase.SammeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.event.KeyEvent

/**
 * 주술사 헬파 사냥 매크로. 무엇을 어떤 순서로 쓸지만 정하고,
 * 초당 횟수, 최소 간격, 사용자 이동키 같은 규칙은 SkillCaster가 지킨다.
 */
class MacroDetailAction2 {
    @Volatile
    private var latestDirection: Int = KeyEvent.VK_LEFT

    fun onDirectionChanged(event: Int) {
        latestDirection = event
    }

    /**
     * 헬파 사냥 (단축키 `). 할 일은 전부 UseCase이고, [MacroEngine]이 상태를 보고 하나씩 골라 실행한다.
     *
     * 우선(앞에서부터, 할 일이 있으면 바로):
     * 공증 > 자힐 > 헬파이어 > 붙은 몹 마비 > 몹 피하기 > 보무 > 마기지체 > 5매각 > 삼매진화
     * - 마비, 몹 피하기, 5매각은 몹 탐지 중에만. 몹 탐지를 켜 두면 삼매진화는 5매각으로만 쓴다.
     * - 저주 돌리기·평타(예전 첨첨 사냥 동작)는 하지 않는다.
     * - 사용자가 방향키로 이동 중이거나 게임 창이 맨 앞이 아니면 아무것도 안 한다.
     */
    suspend fun hellfireHunt() = withContext(Dispatchers.Default) {
        // 마비와 5매각은 서로의 상태(마비 건 몹, 만들고 있는 각)를 보고, 내가 피해서 움직이면 둘 다 기억한 칸을 옮긴다
        lateinit var fiveCross: FiveCrossUseCase
        val mabee = MabeeUseCase(reserved = { fiveCross.reserved })
        fiveCross = FiveCrossUseCase(paralyzed = { mabee.paralyzedTiles })
        val evade = EvadeUseCase(
            // 마비 건 몹은 못 움직이고 못 때리고, 각으로 묶은 몹은 일부러 모아 둔 것이라 피하지 않는다
            ignore = { mabee.paralyzedTiles + fiveCross.reserved },
            onMoved = { step ->
                mabee.onMoved(step)
                fiveCross.onMoved(step)
            },
        )
        MacroEngine(
            priority = listOf(
                ManaUseCase(),
                HealUseCase(),
                HellfireUseCase({ latestDirection }),
                mabee,
                evade,
                BomuUseCase(),
                MagiUseCase(),
                fiveCross,
                SammeUseCase(),
            ),
            rotation = emptyList(),
        ).run()
    }
}
