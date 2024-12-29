package follower.macro

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import common.base.UiStateHolder
import common.robot.Keyboard
import follower.model.BuffState
import follower.model.MagicResultState
import follower.ocr.TextDetecter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

object FollowerMacro {
    private val scope = CoroutineScope(SupervisorJob())

    var job: Job? = null
        private set
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

    private fun heal() {
        val maxCount = 14 // 수시로 조정하자
        var counter = 0

        job?.cancel()
        job = scope.launch {
            launch(Dispatchers.IO) {
                while (isActive) observeBuffState()
            }
            launch(Dispatchers.IO) {
                while (isActive) observeMagicResult()
            }

            macroDetailAction.tabTab()
            while (isActive) {
                Keyboard.pressAndRelease(
                    keyEvent = KeyEvent.VK_1,
                    delay = Random.nextLong(30, 50)
                )

                if(counter > maxCount) {
                    macroDetailAction.tryGongJeung()
                    counter = 0
                }
                counter += 1

                checkBuff()
                checkMagicResult()
            }
        }
    }

    private suspend fun observeBuffState() {
        if(buffState.get() != BuffState.NONE) {
            return
        }
        withContext(Dispatchers.Default) {
            Keyboard.pressAndRelease(KeyEvent.VK_S)
        }
        val uiState = UiStateHolder.state.value
        val rect = uiState.buffState.rectangle

        val text = TextDetecter.detectString(rect)
        val state = when {
            !text.contains(BuffState.INVINSIBILITY.tag) -> BuffState.INVINSIBILITY
//            !text.contains(BuffState.BOMU.tag) -> BuffState.BOMU
            else -> BuffState.NONE
        }

        if(state != BuffState.NONE) {
            println("buffState: $state")
        }

        buffState.set(state)
    }

    private suspend fun observeMagicResult() {
        if(magicResultState.get() != MagicResultState.NONE) {
            return
        }

        val uiState = UiStateHolder.state.value
        val rect = uiState.magicResultState.rectangle

        val text = TextDetecter.detectString(rect)
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
//            BuffState.BOMU -> macroDetailAction.bomu()
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