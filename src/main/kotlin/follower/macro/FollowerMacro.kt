package follower.macro

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import common.UiStateHolder
import common.robot.DisplayProvider
import common.model.MoveEvent
import common.model.UiState
import common.robot.Keyboard
import follower.model.BuffState
import follower.model.MagicResultState
import follower.ocr.TextDetecter
import kotlinx.coroutines.*
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

object FollowerMacro {
    private val scope = CoroutineScope(SupervisorJob())

    private var job: Job? = null

    private val macroDetailAction = MacroDetailAction()
    private val moveDetailAction = MoveDetailAction()

    private val buffState = AtomicReference(BuffState.NONE)
    private val magicResultState = AtomicReference(MagicResultState.NONE)

    internal val property = AtomicBoolean(false)
    internal val ctrlToggle = AtomicBoolean(false)

    suspend fun dispatch(event: MoveEvent) {
        when(event) {
            is MoveEvent.OnCommanderPositionChanged -> {
                moveDetailAction.update(event.point)
            }
            is MoveEvent.OnMove -> {
                val point = UiStateHolder.getCoordinates() ?: return
                moveDetailAction.moveTowards(point)
            }
        }
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

    internal suspend fun obtainProperty() {
        property.set(true)

        listOf(
            KeyEvent.VK_UP,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_LEFT,
            KeyEvent.VK_RIGHT,
        ).forEach {
            Keyboard.release(it)
        }

        withContext(Dispatchers.IO) {
            launch(Dispatchers.IO) {
                delay(1.seconds)
                property.set(false)
            }
        }
    }

    internal fun toggleMoveCtrl() {
        val toggle = !ctrlToggle.get()
        ctrlToggle.set(toggle)
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

        val image = DisplayProvider.capture(UiState.Type.BUFF)
        val text = TextDetecter.detectString(image)

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

    private suspend fun observeMagicResult() {
        if(magicResultState.get() != MagicResultState.NONE) {
            return
        }

        val image = DisplayProvider.capture(UiState.Type.MAGIC_RESULT)
        val text = TextDetecter.detectString(image)

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