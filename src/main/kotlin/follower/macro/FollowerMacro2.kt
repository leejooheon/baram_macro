package follower.macro

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import common.Keyboard
import follower.Follower
import follower.model.BuffState
import follower.model.MagicResultState
import follower.ocr.TextDetecter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicReference

object FollowerMacro2 {
    private val scope = CoroutineScope(SupervisorJob())
    private var job: Job? = null

    private val textDetector = TextDetecter()

    private val macroDetailAction: MacroDetailAction by lazy {
        MacroDetailAction(textDetector)
    }

    private val buffState = AtomicReference(BuffState.NONE)
    private val magicResultState = AtomicReference(MagicResultState.NONE)

    private var buffRect = Rectangle()
    var magicRect = Rectangle()

    fun init(follower: Follower) {
        scope.launch {
            follower.uiState.collectLatest {
                buffRect = it.buffStateRect
                magicRect = it.magicRect
            }
        }
    }
    suspend fun dispatch(keyEvent: Int) {
        when (keyEvent) {
            NativeKeyEvent.VC_ESCAPE -> job?.cancel()
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
            NativeKeyEvent.VC_F4 -> FollowerMacro.invincible()
            NativeKeyEvent.VC_F5 -> FollowerMacro.bomu()
        }
    }

    fun heal() {
        job?.cancel()
        job = scope.launch {
            launch(Dispatchers.IO) {
                while (isActive) checkBuffState()
            }
            launch(Dispatchers.IO) {
                while (isActive) checkMagicResult()
            }

            while (isActive) {
                Keyboard.pressAndRelease(KeyEvent.VK_1)

                val buffState = buffState.get()
                if(buffState != BuffState.NONE) {
                    buff(buffState)
                }

                val magicResultState = magicResultState.get()
                if(magicResultState != MagicResultState.NONE) {
                    magic(magicResultState)
                }
            }
        }
    }

    private suspend fun checkBuffState() {
        if(buffState.get() != BuffState.NONE) {
            return
        }
        withContext(Dispatchers.Default) {
            Keyboard.pressAndRelease(KeyEvent.VK_S)
        }
        val text = textDetector.detectString(buffRect)
        val state = when {
            !text.contains(BuffState.INVINSIBILITY.tag) -> BuffState.INVINSIBILITY
            !text.contains(BuffState.BOMU.tag) -> BuffState.BOMU
            else -> BuffState.NONE
        }

        buffState.set(state)
    }

    private suspend fun checkMagicResult() {
        if(magicResultState.get() != MagicResultState.NONE) {
            return
        }

        val text = textDetector.detectString(magicRect)
        val state = when {
            text.contains(MagicResultState.ME_DEAD.tag) -> MagicResultState.ME_DEAD
            text.contains(MagicResultState.OTHER_DEAD.tag) -> MagicResultState.OTHER_DEAD
            text.contains(MagicResultState.NO_MP.tag) -> MagicResultState.NO_MP
            else -> MagicResultState.NONE
        }
        magicResultState.set(state)
    }

    private suspend fun buff(state: BuffState) {
        when(state) {
            BuffState.INVINSIBILITY -> macroDetailAction.invincible()
            BuffState.BOMU -> macroDetailAction.bomu()
            BuffState.NONE -> { /** nothing **/ }
        }

        buffState.set(BuffState.NONE)
    }

    private suspend fun magic(state: MagicResultState) {
        when(state) {
            MagicResultState.ME_DEAD -> macroDetailAction.dead(true)
            MagicResultState.OTHER_DEAD -> macroDetailAction.dead(false)
            MagicResultState.NO_MP -> macroDetailAction.gongJeung()
            MagicResultState.NONE -> { /** nothing **/ }
        }
        magicResultState.set(MagicResultState.NONE)
    }
}