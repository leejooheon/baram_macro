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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object FollowerMacro {
    private val scope = CoroutineScope(SupervisorJob())
    private var job: Job? = null

    private val macroDetailAction = MacroDetailAction()

    private val buffState = AtomicReference(BuffState.NONE)
    private val magicResultState = AtomicReference(MagicResultState.NONE)
    private var moveState = AtomicBoolean(false)
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

    suspend fun onMoving(pressed: Boolean) = withContext(Dispatchers.IO) {
        moveState.set(pressed)
    }

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
        val maxCount = 20 // 수시로 조정하자
        var counter = 0

        job?.cancel()
        job = scope.launch {
            launch(Dispatchers.IO) {
                while (isActive) checkBuffState()
            }
            launch(Dispatchers.IO) {
                while (isActive) checkMagicResult()
            }

            macroDetailAction.tabTab()
            while (isActive) {
                Keyboard.pressAndRelease(KeyEvent.VK_1)
                if(moveState.get()) continue

                if(counter > maxCount) {
                    macroDetailAction.tryGongJeung()
                    counter = 0
                }
                counter += 1

                maybeBuff()
                maybeMagic()
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
        val text = TextDetecter.detectString(buffRect)
        val state = when {
            !text.contains(BuffState.INVINSIBILITY.tag) -> BuffState.INVINSIBILITY
            !text.contains(BuffState.BOMU.tag) -> BuffState.BOMU
            else -> BuffState.NONE
        }

        if(state != BuffState.NONE) {
            println("buffState: $state")
        }

        buffState.set(state)
    }

    private suspend fun checkMagicResult() {
        if(magicResultState.get() != MagicResultState.NONE) {
            return
        }

        val text = TextDetecter.detectString(magicRect)
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

    private suspend fun maybeBuff() {
        val state = buffState.get()
        if(state == BuffState.NONE) {
            return
        }

        when(state) {
            BuffState.INVINSIBILITY -> macroDetailAction.invincible()
            BuffState.BOMU -> macroDetailAction.bomu()
            else -> { /** nothing **/ }
        }

        buffState.set(BuffState.NONE)
    }

    private suspend fun maybeMagic() {
        val state = magicResultState.get()
        if(state == MagicResultState.NONE) return

        when(state) {
            MagicResultState.ME_DEAD -> macroDetailAction.dead(true)
            MagicResultState.OTHER_DEAD -> macroDetailAction.dead(false)
            MagicResultState.NO_MP -> macroDetailAction.gongJeung()
            else -> { /** nothing **/ }
        }
        magicResultState.set(MagicResultState.NONE)
    }
}