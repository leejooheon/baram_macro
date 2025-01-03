package follower

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import common.UiStateHolder
import common.base.BaseViewModel
import common.model.*
import common.model.KeyEventModel.Companion.toKeyEventModel
import common.model.PointModel.Companion.toPointModel
import common.model.UiState.Type
import common.network.commanderPort
import common.network.host
import common.robot.DisplayProvider
import common.robot.Keyboard
import common.util.Result
import common.util.onError
import common.util.onSuccess
import follower.macro.FollowerMacro
import follower.model.ConnectionState
import follower.model.MacroUiState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.io.File
import java.net.Socket
import java.net.SocketException
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.seconds

class FollowerViewModel: BaseViewModel() {
    private val scope = CoroutineScope(SupervisorJob())
    private var connectionJob: Job? = null

    private val _macroUiState = MutableStateFlow(MacroUiState.default)
    internal val macroUiState = _macroUiState.asStateFlow()

    init {
        init()
        observeScreens()
        observeMacro()
    }

    override fun dispatch(event: UiEvent) = scope.launch {
        when(event) {
            is UiEvent.OnTryConnect -> {
                tryConnect()
            }
            is UiEvent.OnRectangleChanged -> {
                onRectangleChanged(
                    type = event.type,
                    rectangle = event.rectangle
                )
            }
        }
    }

    private suspend fun onRectangleChanged(
        type: Type,
        rectangle: Rectangle,
    ) {
        val state = UiStateHolder.state.value
        val newState = when (type) {
            Type.X -> state.xState.copy(rectangle = rectangle)
            Type.Y -> state.yState.copy(rectangle = rectangle)
            Type.BUFF -> state.buffState.copy(rectangle = rectangle)
            Type.MAGIC_RESULT -> state.magicResultState.copy(rectangle = rectangle)
        }

        UiStateHolder.update(
            type = type,
            state = newState
        )
    }

    private fun tryConnect() {
        connectionJob?.cancel()
        connectionJob = scope.launch(Dispatchers.IO) {
            val state = UiStateHolder.state.value
            if(state.isRunning) {
                return@launch
            }
            UiStateHolder.update(
                isRunning = true,
                connectionState = ConnectionState.Connecting
            )

            val socket = Socket(host, commanderPort)

            UiStateHolder.update(
                isRunning = true,
                connectionState = ConnectionState.Connected
            )
            val reader = socket.getInputStream().bufferedReader()

            // 서버 메시지를 수신하기 위한 루프
            while (isActive) {
                try {
                    val message = reader.readLine() ?: break // 서버 메시지 읽기
                    launch(Dispatchers.IO) {
                        message.toKeyEventModel()?.let { model ->
                            if(model.isPressed) {
                                dispatchKeyPressEvent(model.keyEvent)
                            } else {
                                dispatchKeyReleaseEvent(model.keyEvent)
                            }
                        }
                        message.toPointModel()?.let { model ->
                            val point = Point(model.x, model.y)
                            _macroUiState.update { it.copy(point = point) }
                            FollowerMacro.dispatch(MoveEvent.OnCommanderPositionChanged(point))
                        }
                    }
                } catch (e: SocketException) {
                    onDisconnected()
                }
            }
        }
    }

    private suspend fun onDisconnected() {
        UiStateHolder.update(
            isRunning = false,
            connectionState = ConnectionState.Disconnected
        )
        connectionJob?.cancel()
        connectionJob = null
    }

    private suspend fun dispatchKeyReleaseEvent(keyEvent: Int) {
        when {
            keyEvent == ctrlCommand -> {
                FollowerMacro.toggleMoveCtrl()
            }
            keyEvent in macroCommandFilter -> {
                FollowerMacro.dispatch(keyEvent)
            }

            keyEvent in moveCommandFilter -> {
                val event = when (keyEvent) {
                    NativeKeyEvent.VC_UP -> KeyEvent.VK_UP
                    NativeKeyEvent.VC_LEFT -> KeyEvent.VK_LEFT
                    NativeKeyEvent.VC_DOWN -> KeyEvent.VK_DOWN
                    NativeKeyEvent.VC_RIGHT -> KeyEvent.VK_RIGHT
                    else -> return
                }
                Keyboard.release(event)
            }
        }
    }

    private fun dispatchKeyPressEvent(keyEvent: Int) {
        when {
            keyEvent in moveCommandFilter -> {
                val event = when (keyEvent) {
                    NativeKeyEvent.VC_UP -> KeyEvent.VK_UP
                    NativeKeyEvent.VC_LEFT -> KeyEvent.VK_LEFT
                    NativeKeyEvent.VC_DOWN -> KeyEvent.VK_DOWN
                    NativeKeyEvent.VC_RIGHT -> KeyEvent.VK_RIGHT
                    else -> return
                }
                Keyboard.press(event)
            }
        }
    }

    private fun observeScreens() = scope.launch {
        launch(Dispatchers.IO) {
            while(isActive) {
                updateCoordinates(
                    duration = 0.5.seconds,
                    action = {
                        FollowerMacro.dispatch(MoveEvent.OnMove)
                    }
                )
            }
        }
        launch(Dispatchers.IO) {
            updateFromLocal(
                type = Type.BUFF,
                duration = 1.seconds
            )
        }
        launch(Dispatchers.IO) {
            updateFromLocal(
                type = Type.MAGIC_RESULT,
                duration = 1.seconds
            )
        }
    }

    private fun observeMacro() = scope.launch(Dispatchers.IO) {
        combine(
            FollowerMacro.cycleTime,
            FollowerMacro.ctrlToggle
        ) { time, toggle ->
            time to toggle
        }.collectLatest { (time, toggle) ->
            _macroUiState.update {
                it.copy(
                    cycleTime = time,
                    ctrlToggle = toggle,
                )
            }
        }
    }
    private fun init() = scope.launch {
        FollowerMacro.init(ocrClient)
        UiStateHolder.init(
            UiState.default.copy(
                xState = UiState.CommonState.default.copy(
                    rectangle = Rectangle(2237, 1299, 74, 28),
                    type = Type.X
                ),
                yState = UiState.CommonState.default.copy(
                    rectangle = Rectangle(2303, 1302, 74, 32),
                    type = Type.Y
                ),
                buffState = UiState.CommonState.default.copy(
                    rectangle = Rectangle(2118, 920, 200, 90),
                    type = Type.BUFF
                ),
                magicResultState = UiState.CommonState.default.copy(
                    rectangle = Rectangle(2100, 1134, 256, 40),
                    type = Type.MAGIC_RESULT
                ),
            )
        )
    }
}