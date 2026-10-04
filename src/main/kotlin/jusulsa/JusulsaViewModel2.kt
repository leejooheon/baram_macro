package jusulsa

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import common.robot.Keyboard
import common.robot.UserInput
import jusulsa.model.JusulsaUiState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.awt.event.KeyEvent

class JusulsaViewModel2 {
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        if(exception is CancellationException) {
            runBlocking {
                Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
            }
        }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
    private val macroDetailAction = MacroDetailAction2()
    @Volatile private var actionJob: Job? = null
    private val _uiState = MutableStateFlow(JusulsaUiState.default)
    internal val uiState = _uiState.asStateFlow()

    fun dispatchKeyReleaseEvent(keyEvent: Int) {
        println("dispatch:$keyEvent")
        UserInput.onKey(keyEvent, pressed = false)

        when(keyEvent) {
            NativeKeyEvent.VC_BACKQUOTE -> execute { macroDetailAction.hellfireHunt() }

            NativeKeyEvent.VC_UP -> macroDetailAction.onDirectionChanged(KeyEvent.VK_UP)
            NativeKeyEvent.VC_LEFT -> macroDetailAction.onDirectionChanged(KeyEvent.VK_LEFT)
            NativeKeyEvent.VC_DOWN -> macroDetailAction.onDirectionChanged(KeyEvent.VK_DOWN)
            NativeKeyEvent.VC_RIGHT -> macroDetailAction.onDirectionChanged(KeyEvent.VK_RIGHT)

            NativeKeyEvent.VC_ESCAPE -> actionJob?.cancel()
        }
    }

    private fun execute(block: suspend CoroutineScope.() -> Unit): Job {
        val previous = actionJob
        previous?.cancel()
        return scope.launch {
            // 이전 매크로가 눌린 키를 다 떼고 끝난 뒤에 시작해야 입력이 섞이지 않는다
            previous?.join()
            _uiState.update { it.copy(isRunning = true) }
            try {
                block()
            } finally {
                withContext(NonCancellable) { Keyboard.releaseAll() }
            }
        }.also { job ->
            actionJob = job
            job.invokeOnCompletion {
                if (actionJob === job) _uiState.update { it.copy(isRunning = false) }
            }
        }
    }

    companion object {
        // 매크로 단축키는 게임에 넘기지 않는다. 방향키와 ESC는 게임에서도 써야 하므로 뺀다
        internal val MACRO_KEYS = setOf(
            NativeKeyEvent.VC_BACKQUOTE,
        )
    }
    fun dispatchKeyPressEvent(keyEvent: Int) {
        UserInput.onKey(keyEvent, pressed = true)
    }
}