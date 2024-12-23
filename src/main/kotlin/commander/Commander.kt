package commander

import commander.model.*
import common.model.EventModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import common.model.RingBuffer
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class Commander {
    private val scope = CoroutineScope(SupervisorJob())
    private val commandBuffer = RingBuffer(5) { "" }

    private val serverSocket = ServerSocket(PORT)
    private val clients = mutableListOf<Socket>()

    private val _clientState = MutableStateFlow<List<Socket>>(emptyList())
    val clientState = _clientState.asStateFlow()

    private val _uiState = MutableStateFlow(CommanderUiState.default)
    val uiState = _uiState.asStateFlow()

    private var isCtrlPressed = AtomicBoolean(false)

    init {
        observeClientState()
    }

    private fun observeClientState() = scope.launch {
        clientState.collect { clients ->
            _uiState.update {
                it.copy(
                    commanderState = CommanderState(clients.map { client -> client.inetAddress.hostAddress })
                )
            }
        }
    }

    fun start() = scope.launch(Dispatchers.IO) {
        if(uiState.value.isRunning) return@launch
        _uiState.update { it.copy(isRunning = true) }
        while (true) {
            val client = serverSocket.accept()
            synchronized(clients) {
                clients.add(client)
                _clientState.update { it.plus(client) }
            }
            println("클라이언트 연결됨: ${client.inetAddress.hostAddress}")

            launch(Dispatchers.IO) {
                handleClient(client)
            }
        }
    }

    fun dispatchKeyPressEvent(keyEvent: Int) {
        when {
            keyEvent == ctrlCommand -> {
                isCtrlPressed.set(true)
                return
            }

            isCtrlPressed.get() && keyEvent in ctrlCommandFilter -> {
                val model = EventModel(
                    keyEvent = keyEvent,
                    isPressed = true,
                )
                sendCommand(model.toString())
            }
        }
    }

    fun dispatchKeyReleaseEvent(keyEvent: Int) {
        when {
            keyEvent == ctrlCommand -> {
                isCtrlPressed.set(false)
                return
            }

            keyEvent in ctrlCommandFilter -> {
                val model = EventModel(
                    keyEvent = keyEvent,
                    isPressed = false,
                )
                sendCommand(model.toString())
            }

            keyEvent in macroCommandFilter -> {
                val model = EventModel(
                    keyEvent = keyEvent,
                    isPressed = false,
                )
                sendCommand(model.toString())
            }
        }
    }

    private fun sendCommand(command: String) {
        synchronized(clients) {
            clients.forEach { client ->
                try {
                    val writer = BufferedWriter(OutputStreamWriter(client.getOutputStream()))
                    writer.write(command)
                    writer.newLine()
                    writer.flush()
                } catch (e: Exception) {
//                    clients.remove(client)
                } finally {
                    commandBuffer.append(command)
                    _uiState.update {
                        it.copy(
                            commandBuffer = commandBuffer.toList(),
                        )
                    }
                }
            }
        }
    }

    private fun handleClient(client: Socket) {
        try {
            val input = client.getInputStream().bufferedReader()

            while (true) {
                val message = input.readLine() ?: break // 클라이언트 메시지를 수신
                println("수신된 메시지: $message")
            }
        } catch (e: Exception) {
            println("클라이언트 연결 종료 감지됨: ${client.inetAddress.hostAddress}, 이유: ${e.message}")
        } finally {
            disconnectClient(client)
        }
    }

    private fun disconnectClient(client: Socket) {
        synchronized(clients) {
            clients.remove(client) // 클라이언트 리스트에서 제거
            _clientState.update { it.minus(client) }
        }
        println("클라이언트 연결 해제: ${client.inetAddress.hostAddress}")
    }

    companion object {
        const val PORT = 12345
    }
}

