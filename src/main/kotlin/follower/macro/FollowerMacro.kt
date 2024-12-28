package follower.macro

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import common.robot.Keyboard
import follower.model.BuffState
import follower.model.MagicResultState
import kotlinx.coroutines.*
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicReference

object FollowerMacro {
    private val scope = CoroutineScope(SupervisorJob())
    private var job: Job? = null

    private val macroDetailAction = MacroDetailAction()
    private val buffState = AtomicReference(BuffState.NONE)
    private val magicResultState = AtomicReference(MagicResultState.NONE)

    suspend fun dispatch(keyEvent: Int) {
        when (keyEvent) {
            NativeKeyEvent.VC_ESCAPE -> {
                job?.cancel()
                macroDetailAction.escape()
            }
            NativeKeyEvent.VC_F1 -> heal()
            NativeKeyEvent.VC_BACKQUOTE -> {
                job?.cancel()
                job = scope.launch {
                    macroDetailAction.gongju()
                    heal()
                }
            }
            NativeKeyEvent.VC_F3 -> {
                job?.cancel()
                job = scope.launch {
                    macroDetailAction.honmasul()
                }
            }
            NativeKeyEvent.VC_F4 -> macroDetailAction.invincible()
            NativeKeyEvent.VC_F5 -> macroDetailAction.bomu()
        }
    }

    fun heal() {
        val maxCount = 14 // 수시로 조정하자
        var counter = 0

        job?.cancel()
        job = scope.launch {
            macroDetailAction.tabTab()
            while (isActive) {
                Keyboard.pressAndRelease(KeyEvent.VK_1)

                if(counter++ > maxCount) {
                    macroDetailAction.tryGongJeung()
                    counter = 0
                }

                checkBuff()
                checkMagicResult()
            }
        }
    }

    internal fun onBuffStateUpdate(text: String) {
        val state = when {
            !text.contains(BuffState.INVINSIBILITY.tag) -> BuffState.INVINSIBILITY
            !text.contains(BuffState.BOMU.tag) -> BuffState.BOMU
            else -> BuffState.NONE
        }

        if(state != BuffState.NONE) {
            println("buffState: $buffState")
        }

        buffState.set(state)
    }

    internal fun onMagicResultUpdate(text: String) {
        val state = when {
            text.contains(MagicResultState.ME_DEAD.tag) -> MagicResultState.ME_DEAD
            text.contains(MagicResultState.OTHER_DEAD.tag) -> MagicResultState.OTHER_DEAD
            text.contains(MagicResultState.NO_MP.tag) -> MagicResultState.NO_MP
            else -> MagicResultState.NONE
        }

        if(state != MagicResultState.NONE) {
            println("magicResultState: $state")
        }

        magicResultState.set(state)
    }

    private suspend fun checkBuff() {
        val state = buffState.get()
        if(state == BuffState.NONE) return

        when(state) {
            BuffState.INVINSIBILITY -> macroDetailAction.invincible()
            BuffState.BOMU -> macroDetailAction.bomu()
            else -> { /** nothing **/ }
        }

        buffState.set(BuffState.NONE)
    }

    private suspend fun checkMagicResult() {
        val state = magicResultState.get()
        if(state == MagicResultState.NONE) return

        when(state) {
            MagicResultState.ME_DEAD,
            MagicResultState.OTHER_DEAD, -> macroDetailAction.dead(state)
            MagicResultState.NO_MP -> macroDetailAction.gongJeung()
            else -> { /** nothing **/ }
        }
        magicResultState.set(MagicResultState.NONE)
    }
}