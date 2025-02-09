package commander

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import common.base.BaseViewModel
import common.UiStateHolder
import common.model.*
import common.network.commanderPort
import common.robot.Keyboard
import follower.model.ConnectionState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import kotlin.time.Duration.Companion.seconds
import common.model.UiState.Type
import common.robot.DisplayProvider
import common.util.Result
import java.util.concurrent.atomic.AtomicBoolean

class CommanderViewModel: BaseViewModel() {
    private val scope = CoroutineScope(SupervisorJob())

    private val serverSocket = ServerSocket(commanderPort)
    private var client:Socket? = null

    private var connectionJob: Job? = null

    private val movePressed = AtomicBoolean(false)
    init {
        observeScreens()
        init()
    }

    override fun dispatch(event: UiEvent) = scope.launch {
        when(event) {
            is UiEvent.OnTryConnect -> {
                start()
            }
            is UiEvent.OnRectangleChanged -> onRectangleChanged(
                type = event.type,
                rectangle = event.rectangle
            )
        }
    }

    private fun start() {
        connectionJob?.cancel()
        connectionJob = scope.launch(Dispatchers.IO) {
            val state = UiStateHolder.state.value
            if(state.isRunning) return@launch
            UiStateHolder.update(
                isRunning = true,
                connectionState = ConnectionState.Connecting
            )

            while (isActive) {
                val client = serverSocket.accept().also {
                    this@CommanderViewModel.client = it
                }
                UiStateHolder.update(
                    isRunning = true,
                    connectionState = ConnectionState.Connected
                )
                println("connected: ${client.inetAddress.hostAddress}")

                launch(Dispatchers.IO) {
                    handleClient(client)
                }
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

    fun dispatchKeyReleaseEvent(keyEvent: Int) = scope.launch {
        when {
            keyEvent in macroCommandFilter.plus(ctrlCommand) -> {
                val model = KeyEventModel(
                    keyEvent = keyEvent,
                    isPressed = false
                )
                sendCommand(model.toString())
            }
            keyEvent in oneHandFilter -> {
                scope.launch {
                    when(keyEvent) {
                        NativeKeyEvent.VC_PAGE_UP -> {
                            Keyboard.pressAndRelease(KeyEvent.VK_F1)
                        }
                        NativeKeyEvent.VC_PAGE_DOWN -> {
                            Keyboard.pressAndRelease(KeyEvent.VK_F3)
                        }
                    }
                }
            }
            keyEvent == moveCommand -> {
                movePressed.set(false)
                val model = KeyEventModel(
                    keyEvent = ctrlCommand,
                    isPressed = false
                )
                sendCommand(model.toString())
            }
            keyEvent in moveCommandFilter && movePressed.get() -> {
                val model = KeyEventModel(
                    keyEvent = keyEvent,
                    isPressed = false
                )
                sendCommand(model.toString())
            }
        }
    }
    fun dispatchKeyPressEvent(keyEvent: Int) = scope.launch {
        when {
            keyEvent == moveCommand -> {
                if(!movePressed.getAndSet(true)) {
                    val model = KeyEventModel(
                        keyEvent = ctrlCommand,
                        isPressed = false
                    )
                    sendCommand(model.toString())
                }
            }
            keyEvent in moveCommandFilter && movePressed.get() -> {
                val model = KeyEventModel(
                    keyEvent = keyEvent,
                    isPressed = true
                )
                sendCommand(model.toString())
            }
        }
    }

    private suspend fun sendCommand(command: String) = withContext(Dispatchers.IO) {
        val client = client ?: return@withContext

        try {
            val writer = BufferedWriter(OutputStreamWriter(client.getOutputStream()))
            writer.write(command)
            writer.newLine()
            writer.flush()
        } catch (e: Exception) {
            disconnectClient()
        }
    }

    private suspend fun handleClient(client: Socket) = withContext(Dispatchers.IO) {
        try {
            val input = client.getInputStream().bufferedReader()

            while (true) {
                val message = input.readLine() ?: break // 클라이언트 메시지를 수신
                println("수신된 메시지: $message")
            }
        } catch (e: Exception) {
            println("클라이언트 연결 종료 감지됨: ${client.inetAddress.hostAddress}, 이유: ${e.message}")
        } finally {
            disconnectClient()
        }
    }

    private suspend fun disconnectClient() {
        println("클라이언트 연결 해제: ${client?.inetAddress?.hostAddress}")

        UiStateHolder.update(
            connectionState = ConnectionState.Disconnected,
            isRunning = false
        )
        client = null
    }

    private fun observeScreens() = scope.launch {
        launch(Dispatchers.IO) {
            while(isActive) {
                updateCoordinates(
                    duration = 1.seconds,
                    action = { (x, y) ->
                        val intX = x.toIntOrNull() ?: return@updateCoordinates
                        val intY = y.toIntOrNull() ?: return@updateCoordinates
                        sendCommand(PointModel(intX, intY).toString())
                    }
                )
            }
        }
    }
    private fun init() = scope.launch {
        UiStateHolder.init(
            UiState.default.copy(
                xState = UiState.CommonState.default.copy(
                    rectangle = Rectangle(1330, 817, 80, 30)
                ),
                yState = UiState.CommonState.default.copy(
                    rectangle = Rectangle(1410, 815, 80, 30)
                ),
            )
        )
    }
}