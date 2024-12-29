package follower

import common.model.ctrlCommand
import common.model.macroCommandFilter
import common.UiStateHolder
import common.base.BaseViewModel
import common.model.MoveEvent
import common.model.UiEvent
import common.model.KeyEventModel.Companion.toKeyEventModel
import common.model.PointModel.Companion.toPointModel
import common.model.UiState
import common.model.UiState.Type
import common.network.commanderPort
import common.network.host
import follower.macro.FollowerMacro
import follower.model.ConnectionState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.awt.Point
import java.awt.Rectangle
import java.net.Socket
import java.net.SocketException
import kotlin.time.Duration.Companion.seconds

class FollowerViewModel: BaseViewModel() {
    private val scope = CoroutineScope(SupervisorJob())
    private var connectionJob: Job? = null

    private val _commanderPoint = MutableStateFlow(Point(0, 0))
    internal val commanderPoint = _commanderPoint.asStateFlow()

    init {
        init()
        observeScreens()
        observeCoordinates()
    }

    override fun dispatch(event: UiEvent) = scope.launch {
        when(event) {
            is UiEvent.OnTryConnect -> {
//                test()
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
                    launch(Dispatchers.Default) {
                        message.toKeyEventModel()?.let { model ->
                            dispatchKeyReleaseEvent(model.keyEvent)
                        }
                        message.toPointModel()?.let { model ->
                            val point = Point(model.x, model.y)

                            _commanderPoint.emit(point)
                            FollowerMacro.dispatch(MoveEvent.OnCommanderPositionChanged(point))
                            FollowerMacro.dispatch(MoveEvent.OnMove)
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
        }
    }

    private fun observeScreens() = scope.launch {
        launch {
            updateFromRemote(
                type = Type.X,
                duration = 1.seconds
            )
        }
        launch {
            updateFromRemote(
                type = Type.Y,
                duration = 1.seconds
            )
        }

        launch {
            updateFromLocal(
                type = Type.BUFF,
                duration = 1.seconds
            )
        }
        launch {
            updateFromLocal(
                type = Type.MAGIC_RESULT,
                duration = 1.seconds
            )
        }
    }

    private fun observeCoordinates() = scope.launch {
        UiStateHolder.state
            .map {
                val x = it.xState.texts
                val y = it.yState.texts
                x to y
            }
            .distinctUntilChanged { old, new ->
                old.first == new.first && old.second == new.second
            }
            .collectLatest {
                FollowerMacro.dispatch(MoveEvent.OnMove)
            }
    }

    private fun init() = scope.launch {
        UiStateHolder.init(
            UiState.default.copy(
                xState = UiState.CommonState.default.copy(
                    rectangle = Rectangle(2205, 1310, 75, 28),
                    type = Type.X
                ),
                yState = UiState.CommonState.default.copy(
                    rectangle = Rectangle(2285, 1310, 75, 28),
                    type = Type.Y
                ),
                buffState = UiState.CommonState.default.copy(
                    rectangle = Rectangle(2070, 890, 256, 128),
                    type = Type.BUFF
                ),
                magicResultState = UiState.CommonState.default.copy(
                    rectangle = Rectangle(2050, 1130, 256, 48),
                    type = Type.MAGIC_RESULT
                ),
            )
        )
    }
}