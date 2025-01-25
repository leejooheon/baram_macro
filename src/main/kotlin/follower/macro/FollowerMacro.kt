package follower.macro

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import common.UiStateHolder
import common.model.MoveEvent
import common.model.UiState
import common.robot.DisplayProvider
import common.robot.Keyboard
import follower.model.BuffState
import follower.model.MagicResultState
import follower.ocr.TextDetecter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.event.KeyEvent
import java.util.*
import kotlin.concurrent.schedule
import kotlin.time.Duration.Companion.seconds

object FollowerMacro {
    private val scope = CoroutineScope(SupervisorJob())

    private var healJob: Job? = null
    private var moveJob: Job? = null

    private val macroDetailAction = MacroDetailAction()
    private lateinit var moveDetailAction: MoveDetailAction

    private var magicResultState = MagicResultState.NONE

    internal var property = false

    private val _cycleTime = MutableStateFlow<Long>(0)
    internal val cycleTime = _cycleTime.asStateFlow()

    private val _ctrlToggle = MutableStateFlow(false)
    internal val ctrlToggle = _ctrlToggle.asStateFlow()

    private var timer: TimerTask? = null
    private var invokeHonmasul = false

    fun init() {
        moveDetailAction = MoveDetailAction()
    }

    fun dispatch(event: MoveEvent) {
        when(event) {
            is MoveEvent.OnCommanderPositionChanged -> {
                moveDetailAction.updateCommander(event.point)
            }
            is MoveEvent.OnMove -> {
                val point = UiStateHolder.getCoordinates() ?: return
                moveDetailAction.updateMe(point)

                if(moveJob?.isActive == null || moveJob?.isActive == false) {
                    moveJob?.cancel()
                    moveJob = null
                    moveJob = scope.launch {
                        moveDetailAction.moveTowards()
                    }
                }
            }
        }
    }

    suspend fun dispatch(keyEvent: Int) {
        when (keyEvent) {
            NativeKeyEvent.VC_ESCAPE -> {
                healJob?.cancel()
                macroDetailAction.escape()
                obtainProperty(0)
            }
            NativeKeyEvent.VC_F1 -> heal()
            NativeKeyEvent.VC_F2 -> invokeHonmasul = true
            NativeKeyEvent.VC_BACKQUOTE -> {
                healJob?.cancel()
                healJob = scope.launch {
                    macroDetailAction.gongju()
                    heal()
                }
            }
            NativeKeyEvent.VC_F3 -> {
                healJob?.cancel()
                healJob = scope.launch {
                    macroDetailAction.honmasul()
                }
            }
            NativeKeyEvent.VC_F4 -> macroDetailAction.invincible()
            NativeKeyEvent.VC_F5 -> macroDetailAction.bomu()
        }
    }

    internal fun obtainProperty(delay: Long = 500) {
        timer?.cancel()
        timer = null

        property = true
        moveJob?.cancel()
        moveJob = null

        moveDetailAction.releaseAll()
        timer = Timer().schedule(delay) {
            property = false
        }
    }

    internal suspend fun toggleMoveCtrl() {
        val toggle = !ctrlToggle.value
        _ctrlToggle.emit(toggle)
    }

    private fun heal() {
        obtainProperty()
        healJob?.cancel()
        healJob = scope.launch(Dispatchers.IO) {
            var running = true

            launch(Dispatchers.IO) { // healJob
                val maxCount = 4 // 수시로 조정하자
                var counter = 0
                macroDetailAction.tabTab()
                while (isActive) {
                    if(!running) {
                        delay(1.seconds)
                        continue
                    }
                    if(invokeHonmasul) {
                        invokeHonmasul = false
                        macroDetailAction.honmasul(1.seconds)
                    }
                    macroDetailAction.heal(5)
                    if(counter++ > maxCount) {
                        Keyboard.pressAndRelease(KeyEvent.VK_2) // 공증
                        Keyboard.pressAndRelease(KeyEvent.VK_3) // 희원
                        counter = 0
                    }
                }
            }

            launch(Dispatchers.IO) { // buffJob
                Keyboard.pressAndRelease(KeyEvent.VK_S)
                while (isActive) {
                    if(running) observeBuffState()
                    delay(1.seconds)
                }
            }

            launch(Dispatchers.IO) { // magicResultJob
                while (isActive) {
                    observeMagicResult(
                        onStart = { running = true },
                        onStop = { running = false },
                    )
                    delay(0.5.seconds)
                }
            }
        }
    }

    private suspend fun observeBuffState() {
        val image = DisplayProvider.capture(UiState.Type.BUFF)
        val text = TextDetecter.detectString(image)

        when {
            !text.contains(BuffState.INVINSIBILITY.tag) -> macroDetailAction.invincible()
//            !text.contains(BuffState.BOMU.tag) -> macroDetailAction.bomu()
            else -> { /** nothing **/ }
        }
    }

    private suspend fun observeMagicResult(
        onStart: () -> Unit,
        onStop: () -> Unit,
    ) {
        val image = DisplayProvider.capture(UiState.Type.MAGIC_RESULT)
        val text = TextDetecter.detectString(image)

        val state = when {
            text.contains(MagicResultState.ME_DEAD.tag) -> MagicResultState.ME_DEAD
            text.contains(MagicResultState.OTHER_DEAD.tag) -> MagicResultState.OTHER_DEAD
            text.contains(MagicResultState.NO_MP.tag) -> MagicResultState.NO_MP
            else -> MagicResultState.NONE
        }

        if(state != magicResultState) {
            println("magicResultState: $state")
        }

        when(state) {
            MagicResultState.ME_DEAD,
            MagicResultState.OTHER_DEAD -> {
                onStop.invoke()
                obtainProperty()
                macroDetailAction.dead(state)
                onStart.invoke()
            }
            MagicResultState.NO_MP -> {
                onStop.invoke()
                macroDetailAction.gongJeung()
                onStart.invoke()
            }
            else -> { /** nothing **/ }
        }
    }
}