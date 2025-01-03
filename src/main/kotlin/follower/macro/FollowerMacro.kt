package follower.macro

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import common.UiStateHolder
import common.model.MoveEvent
import common.model.UiState
import common.network.OcrClient
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
import kotlin.random.Random

object FollowerMacro {
    private val scope = CoroutineScope(SupervisorJob())

    private var job: Job? = null
    private var moveJob: Job? = null

    private val macroDetailAction = MacroDetailAction()
    private lateinit var moveDetailAction: MoveDetailAction

    private var buffState = BuffState.NONE
    private var magicResultState = MagicResultState.NONE

    internal var property = false

    private val _cycleTime = MutableStateFlow<Long>(0)
    internal val cycleTime = _cycleTime.asStateFlow()

    private val _ctrlToggle = MutableStateFlow(false)
    internal val ctrlToggle = _ctrlToggle.asStateFlow()

    private var timer: TimerTask? = null
    fun init(
        ocrClient: OcrClient
    ) {
        moveDetailAction = MoveDetailAction(scope, ocrClient)
    }
    suspend fun dispatch(event: MoveEvent) {
        when(event) {
            is MoveEvent.OnCommanderPositionChanged -> {
                moveDetailAction.update(event.point)
            }
            is MoveEvent.OnMove -> {
                val point = UiStateHolder.getCoordinates() ?: return
                moveJob?.cancel()
                moveJob = null
                moveJob =  scope.launch {
                    moveDetailAction.moveTowards(point)
                }
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

    internal fun obtainProperty() {
        timer?.cancel()
        timer = null

        property = true

        moveDetailAction.releaseAll()
        timer = Timer().schedule(500) {
            property = false
        }
    }

    internal suspend fun toggleMoveCtrl() {
        val toggle = !ctrlToggle.value
        _ctrlToggle.emit(toggle)
    }

    private fun heal() {
        val maxCount = 4 // 수시로 조정하자
        var counter = 0

        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            launch(Dispatchers.IO) {
                while (isActive) observeBuffState()
            }
            launch(Dispatchers.IO) {
                while (isActive) observeMagicResult()
            }

            macroDetailAction.tabTab()
            while (isActive) {
                Keyboard.pressKeyRepeatedly(
                    keyEvent = KeyEvent.VK_1,
                    time = 2,
                    delay = Random.nextLong(30, 50)
                )

                if(counter++ > maxCount) {
                    macroDetailAction.tryGongJeung()
                    counter = 0
                }

                checkBuff()
                checkMagicResult()
                delay(Random.nextLong(100, 200))
            }
        }
    }

    private suspend fun observeBuffState() {
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

        if(state != buffState) {
            println("buffState: $state")
        }

        buffState = state
    }

    private suspend fun observeMagicResult() {
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

        magicResultState = state
    }

    private suspend fun checkBuff() {
        val state = buffState
        if(state == BuffState.NONE) return

        when(state) {
            BuffState.INVINSIBILITY -> macroDetailAction.invincible()
            BuffState.BOMU -> macroDetailAction.bomu()
            else -> { /** nothing **/ }
        }
    }

    private suspend fun checkMagicResult() {
        val state = magicResultState
        when(state) {
            MagicResultState.ME_DEAD,
            MagicResultState.OTHER_DEAD -> {
                obtainProperty()
                macroDetailAction.dead(state)
            }
            MagicResultState.NO_MP -> {
                obtainProperty()
                macroDetailAction.gongJeung()
            }
            else -> { /** nothing **/ }
        }
    }
}