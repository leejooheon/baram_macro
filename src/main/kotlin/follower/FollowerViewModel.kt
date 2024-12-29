package follower

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import commander.model.ctrlCommandFilter
import commander.model.macroCommandFilter
import common.base.BaseViewModel
import common.base.UiStateHolder
import common.display.DisplayProvider
import common.event.UiEvent
import common.model.EventModel.Companion.toEventModel
import common.model.PointModel
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
import follower.macro.MoveDetailAction
import follower.model.ConnectionState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.net.Socket
import java.net.SocketException
import kotlin.time.Duration.Companion.seconds

class FollowerViewModel: BaseViewModel() {
    private val scope = CoroutineScope(SupervisorJob())

    private val moveDetailAction by lazy { MoveDetailAction(scope) }
    private val ocrClient = OcrClient(createHttpClient())
    private var connectionJob: Job? = null

    init {
        observeScreens()
        observeCoordinates()
        init()
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
                        message.toEventModel()?.let { model ->
                            if (model.isPressed) {
                                dispatchKeyPressEvent(model.keyEvent)
                            } else {
                                dispatchKeyReleaseEvent(model.keyEvent)
                            }
                        }
                        message.toPointModel()?.let { model ->
                            println("commander Coordinates: $model")
                            moveDetailAction.update(Point(model.x, model.y))
                            moveDetailAction.moveTowards(UiStateHolder.getCoordinates())
                        }
                    }
                } catch (e: SocketException) {
                    onDisconnected()
                }
            }
        }
    }
    private fun onDisconnected() {
        UiStateHolder.update(
            isRunning = false,
            connectionState = ConnectionState.Disconnected
        )
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
        launch(Dispatchers.IO) {
            val displayProvider = DisplayProvider(Keyboard.robot)
            while (isActive) {
                val rect = UiStateHolder.state.value.xState.rectangle
                val screen = displayProvider.capture(rect)
                ocrClient
                    .readImage(screen)
                    .onSuccess {
                        val state = UiStateHolder.state.value
                        UiStateHolder.update(
                            type = Type.X,
                            state.xState.copy(
                                texts = it.results,
                                image = screen
                            )
                        )
                    }
                    .onError {
                        updateImage(
                            type = Type.X,
                            image = screen,
                            texts = emptyList()
                        )
                    }
                delay(0.5.seconds)
            }
        }
        launch {
            val displayProvider = DisplayProvider(Keyboard.robot)
            while (isActive) {
                val rect = UiStateHolder.state.value.yState.rectangle
                val screen = displayProvider.capture(rect)
                ocrClient
                    .readImage(screen)
                    .onSuccess {
                        val state = UiStateHolder.state.value
                        UiStateHolder.update(
                            type = Type.Y,
                            state.yState.copy(
                                texts = it.results,
                                image = screen
                            )
                        )
                    }
                    .onError {
                        updateImage(
                            type = Type.Y,
                            image = screen,
                            texts = emptyList()
                        )
                    }
                delay(0.5.seconds)
            }
        }
//        launch {
//            captureScreen(
//                type = Type.BUFF,
//                duration = 1.seconds
//            ).collectLatest {
//                updateImage(
//                    image = it,
//                    texts = emptyList(),
//                    type = Type.BUFF
//                )
//            }
//        }
//        launch {
//            captureScreen(
//                type = Type.MAGIC_RESULT,
//                duration = 1.seconds
//            ).collectLatest {
//                updateImage(
//                    image = it,
//                    texts = emptyList(),
//                    type = Type.MAGIC_RESULT
//                )
//            }
//        }
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
            .collect {
                    moveDetailAction.moveTowards(UiStateHolder.getCoordinates())
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