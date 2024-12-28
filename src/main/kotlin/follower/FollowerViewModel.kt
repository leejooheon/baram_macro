package follower

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import commander.model.ctrlCommandFilter
import commander.model.macroCommandFilter
import common.base.BaseViewModel
import common.event.UiEvent
import common.model.EventModel.Companion.toEventModel
import common.model.PointModel.Companion.toPointModel
import common.model.Type
import common.model.UiState
import common.network.OcrClient
import common.network.commanderPort
import common.network.createHttpClient
import common.network.host
import common.robot.Keyboard
import common.util.onError
import common.util.onSuccess
import follower.macro.FollowerMacro
import follower.model.ConnectionState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.net.Socket
import java.net.SocketException
import kotlin.time.Duration.Companion.seconds

class FollowerViewModel: BaseViewModel() {
    private val scope = CoroutineScope(SupervisorJob())

    override val _uiState: MutableStateFlow<UiState> =
        MutableStateFlow(
            UiState.default.copy(
                xState = UiState.CommonState.default.copy(
                    rectangle = Rectangle(1330, 815, 80, 30)
                ),
                yState = UiState.CommonState.default.copy(
                    rectangle = Rectangle(1410, 815, 80, 30)
                ),
                buffState = UiState.CommonState.default.copy(
                    rectangle = Rectangle(1180, 370, 256, 128)
                ),
                magicResultState = UiState.CommonState.default.copy(
                    rectangle = Rectangle(1180, 620, 256, 64)
                ),
            )
        )
    override val uiState = _uiState.asStateFlow()

    private val ocrClient = OcrClient(createHttpClient())
    private var connectionJob: Job? = null

    init {
        observeScreens()
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

    private fun onRectangleChanged(
        type: Type,
        rectangle: Rectangle,
    ) {
        _uiState.update {
            when (type) {
                Type.X -> it.copy(xState = it.xState.copy(rectangle = rectangle))
                Type.Y -> it.copy(yState = it.yState.copy(rectangle = rectangle))
                Type.BUFF -> it.copy(buffState = it.buffState.copy(rectangle = rectangle))
                Type.MAGIC_RESULT -> it.copy(magicResultState = it.magicResultState.copy(rectangle = rectangle))
            }
        }
    }

    private suspend fun test() {
        val image = Keyboard.test(Rectangle(0, 0, 100, 50))

        ocrClient
            .readImage(image)
            .onSuccess {
                println("success: ${it.results}")
            }
            .onError {
                println("error")
            }
    }

    private fun tryConnect() {

        connectionJob?.cancel()
        connectionJob = scope.launch(Dispatchers.IO) {
            if(_uiState.value.isRunning) {
                return@launch
            }

            _uiState.update {
                it.copy(
                    isRunning = true,
                    connectionState = ConnectionState.Connecting
                )
            }

            val socket = Socket(host, commanderPort)

            _uiState.update {
                it.copy(connectionState = ConnectionState.Connected)
            }

            val reader = socket.getInputStream().bufferedReader()

            // 서버 메시지를 수신하기 위한 루프
            while (isActive) {
                try {
                    val message = reader.readLine() ?: break // 서버 메시지 읽기
                    launch(Dispatchers.Default) {
                        message.toEventModel()?.let { model ->
                            if (model.isPressed) {
                                dispatchKeyPressEvent(model.keyEvent)
                            } else {
                                dispatchKeyReleaseEvent(model.keyEvent)
                            }
                        }
                        message.toPointModel()?.let { model ->
                            println("receive!! $model")
                        }
                    }
                } catch (e: SocketException) {
                    onDisconnected()
                }
            }
        }
    }
    private fun onDisconnected() {
        _uiState.update {
            it.copy(
                isRunning = false,
                connectionState = ConnectionState.Disconnected
            )
        }

        connectionJob?.cancel()
        connectionJob = null
    }

    private fun dispatchKeyPressEvent(keyEvent: Int) {
        when {
            keyEvent in ctrlCommandFilter -> {
                val event = when (keyEvent) {
                    NativeKeyEvent.VC_W -> KeyEvent.VK_UP
                    NativeKeyEvent.VC_A -> KeyEvent.VK_LEFT
                    NativeKeyEvent.VC_S -> KeyEvent.VK_DOWN
                    NativeKeyEvent.VC_D -> KeyEvent.VK_RIGHT
                    else -> return
                }
                Keyboard.press(event)
            }
        }
    }

    private suspend fun dispatchKeyReleaseEvent(keyEvent: Int) {
        when {
            keyEvent in ctrlCommandFilter -> {
                val event = when (keyEvent) {
                    NativeKeyEvent.VC_W -> KeyEvent.VK_UP
                    NativeKeyEvent.VC_A -> KeyEvent.VK_LEFT
                    NativeKeyEvent.VC_S -> KeyEvent.VK_DOWN
                    NativeKeyEvent.VC_D -> KeyEvent.VK_RIGHT
                    else -> return
                }
                Keyboard.release(event)
            }
            keyEvent in macroCommandFilter -> {
                FollowerMacro.dispatch(keyEvent)
            }
        }
    }

    private fun observeScreens() = scope.launch {
        launch {
            observeAndUpdate(
                type = Type.X,
                duration = 1.seconds
            )
        }
        launch {
            observeAndUpdate(
                type = Type.Y,
                duration = 1.seconds
            )
        }
        launch {
            observeAndUpdate(
                type = Type.BUFF,
                duration = 1.seconds
            )
        }
        launch {
            observeAndUpdate(
                type = Type.MAGIC_RESULT,
                duration = 1.seconds
            )
        }
    }
}