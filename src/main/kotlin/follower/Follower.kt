package follower

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.sun.tools.jconsole.JConsoleContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import commander.Commander.Companion.PORT
import commander.model.ctrlCommandFilter
import commander.model.macroCommandFilter
import common.EventModel.Companion.toModel
import common.Keyboard
import common.RingBuffer
import follower.model.ConnectionState
import follower.model.FollowerUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.awt.event.KeyEvent
import java.net.Socket

class Follower {
    private val scope = CoroutineScope(SupervisorJob())
    private val commandBuffer = RingBuffer(5) { "" }

    private val _uiState = MutableStateFlow(FollowerUiState.default)
    val uiState = _uiState.asStateFlow()

    fun start() = scope.launch(Dispatchers.IO) {
        if(_uiState.value.isRunning) {
            return@launch
        }

        _uiState.update {
            it.copy(
                isRunning = true,
                connectionState = ConnectionState.Connecting
            )
        }

        val socket = Socket("localhost", PORT)

        _uiState.update {
            it.copy(connectionState = ConnectionState.Connected)
        }

        val reader = socket.getInputStream().bufferedReader()

        // 서버 메시지를 수신하기 위한 루프
        while (true) {
            val message = reader.readLine() ?: break // 서버 메시지 읽기
            commandBuffer.append(message)
            _uiState.update {
                it.copy(commandBuffer = commandBuffer.toList())
            }
            scope.launch(Dispatchers.Default) {
                try {
                    val model = message.toModel()
                    println("서버로부터 메시지 수신: $model")

                    if (model.isPressed) {
                        dispatchKeyPressEvent(model.keyEvent)
                    } else {
                        dispatchKeyReleaseEvent(model.keyEvent)
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    private fun dispatchKeyPressEvent(keyEvent: Int) {
        when {
            keyEvent in ctrlCommandFilter -> {
                val event = when (keyEvent) {
                    NativeKeyEvent.VC_W -> KeyEvent.VK_W
                    NativeKeyEvent.VC_A -> KeyEvent.VK_A
                    NativeKeyEvent.VC_S -> KeyEvent.VK_S
                    NativeKeyEvent.VC_D -> KeyEvent.VK_D
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
                    NativeKeyEvent.VC_W -> KeyEvent.VK_W
                    NativeKeyEvent.VC_A -> KeyEvent.VK_A
                    NativeKeyEvent.VC_S -> KeyEvent.VK_S
                    NativeKeyEvent.VC_D -> KeyEvent.VK_D
                    else -> return
                }
                Keyboard.release(event)
            }
            keyEvent in macroCommandFilter -> {
                when (keyEvent) {
                    NativeKeyEvent.VC_ESCAPE -> FollowerMacro.cancelAll()
                    NativeKeyEvent.VC_BACKQUOTE -> FollowerMacro.healMe()
                    NativeKeyEvent.VC_F1 -> FollowerMacro.heal()
                    NativeKeyEvent.VC_F2 -> FollowerMacro.gongJeung()
                    NativeKeyEvent.VC_F3 -> FollowerMacro.honmasul()
                    NativeKeyEvent.VC_F4 -> FollowerMacro.invincible()
                    NativeKeyEvent.VC_F5 -> FollowerMacro.bomu()
                }
            }
        }
    }
}