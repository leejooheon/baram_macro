package follower

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import commander.Commander.Companion.PORT
import commander.model.ctrlCommandFilter
import commander.model.macroCommandFilter
import common.EventModel.Companion.toModel
import common.Keyboard
import common.RingBuffer
import follower.macro.FollowerMacro
import follower.model.ConnectionState
import follower.model.FollowerUiState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.net.Socket
import java.net.SocketException

class Follower {
    private val scope = CoroutineScope(SupervisorJob())
    private val commandBuffer = RingBuffer(5) { "" }

    private val _uiState = MutableStateFlow(FollowerUiState.default)
    val uiState = _uiState.asStateFlow()

    private var connectionJob: Job? = null

    init {
//        FollowerMacro.init(this)
        FollowerMacro.init(this)
    }

    fun start() {
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

            val socket = Socket("192.168.0.2", PORT)

            _uiState.update {
                it.copy(connectionState = ConnectionState.Connected)
            }

            val reader = socket.getInputStream().bufferedReader()

            // 서버 메시지를 수신하기 위한 루프
            while (isActive) {
                try {
                    val message = reader.readLine() ?: break // 서버 메시지 읽기
                    commandBuffer.append(message)
                    _uiState.update {
                        it.copy(commandBuffer = commandBuffer.toList())
                    }
                    launch(Dispatchers.Default) {

                        val model = message.toModel()
                        println("서버로부터 메시지 수신: $model")

                        if (model.isPressed) {
                            dispatchKeyPressEvent(model.keyEvent)
                        } else {
                            dispatchKeyReleaseEvent(model.keyEvent)
                        }
                    }
                } catch (e: SocketException) {
                    onDisconnected()
                }
            }
        }
    }

    fun onMagicRectChanged(rect: Rectangle) {
        _uiState.update {
            it.copy(magicRect = rect)
        }
    }
    fun onBuffStateRectChanged(rect: Rectangle) {
        _uiState.update {
            it.copy(buffStateRect = rect)
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

    suspend fun dispatchKeyReleaseEvent(keyEvent: Int) {
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
}