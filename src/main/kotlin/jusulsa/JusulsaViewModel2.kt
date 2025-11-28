package jusulsa

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import common.base.BaseViewModel
import common.model.UiEvent
import common.robot.DisplayProvider
import common.robot.Keyboard
import follower.macro.MacroDetailAction2
import follower.ocr.TextDetecter
import jusulsa.model.JusulsaUiState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class JusulsaViewModel2 : BaseViewModel() {
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        if(exception is CancellationException) {
            runBlocking {
                Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
            }
        }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined + exceptionHandler)
    private val macroDetailAction = MacroDetailAction2()
    private var actionJob: Job? = null
    private val _uiState = MutableStateFlow(JusulsaUiState.default)
    internal val uiState = _uiState.asStateFlow()
    private val hellfireCount = MutableStateFlow(0)

    init {
        observeScreens()
    }

    fun dispatchKeyReleaseEvent(keyEvent: Int) {
        println("dispatch:$keyEvent")

        when(keyEvent) {
            NativeKeyEvent.VC_PAGE_UP -> hellfire()
            NativeKeyEvent.VC_PAGE_DOWN -> execute { macroDetailAction.mabeAroundMe() }

            NativeKeyEvent.VC_BACKQUOTE -> execute { macroDetailAction.samme() }
            NativeKeyEvent.VC_SLASH -> execute { macroDetailAction.mabeAroundMe() }
            NativeKeyEvent.VC_BACK_SLASH -> execute { macroDetailAction.maagi() }
            NativeKeyEvent.VC_KANJI -> execute { macroDetailAction.jeoju() }

            NativeKeyEvent.VC_1 -> execute { macroDetailAction.heal() }
            NativeKeyEvent.VC_2 -> execute { macroDetailAction.mabee() }
            NativeKeyEvent.VC_3 -> execute { macroDetailAction.julmang() }

            NativeKeyEvent.VC_F1 -> execute { macroDetailAction.hondon() }

            NativeKeyEvent.VC_UP,
            NativeKeyEvent.VC_LEFT,
            NativeKeyEvent.VC_DOWN,
            NativeKeyEvent.VC_RIGHT -> macroDetailAction.onDirectionChanged(keyEvent)

            NativeKeyEvent.VC_ESCAPE -> actionJob?.cancel()
        }
    }

    private fun hellfire() {
        hellfireCount.value += 1

        if (actionJob?.isActive == true) return
        else hellfireCount.value = 1

        execute {
            while (hellfireCount.value > 0) {
                hellfireCount.value -= 1
                hellfireInternal()
            }
        }
    }

    private suspend fun hellfireInternal() {
        macroDetailAction.hellfire()
        val startTime = System.currentTimeMillis()
        if (checkHellfireDelay()) {
            macroDetailAction.gongjeung()
            val consumedTime = System.currentTimeMillis() - startTime
            val healTime = 7.seconds.inWholeMilliseconds - consumedTime

            runCatching {
                withTimeout(healTime) { macroDetailAction.heal() }
            }.onFailure {
                Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
            }
        }
    }

    private fun observeScreens() = scope.launch {
        launch {
            updateFromLocal(
                state = uiState.value.addOnState,
                duration = 1.seconds
            )
        }
        launch {
            hellfireCount.collectLatest {
                _uiState.value = _uiState.value.copy(count = it)
            }
        }
    }

    private suspend fun checkHellfireDelay(): Boolean {
        delay(200)
        repeat(2) {
            val screen = DisplayProvider.capture2(uiState.value.addOnState.rectangle)
            val text = TextDetecter.detectString(screen)
            updateScreen(uiState.value.addOnState, screen, text)
            if (text.contains("헬")) return true
        }
        return false
    }

    private fun updateScreen(
        state: JusulsaUiState.State,
        screen: BufferedImage,
        text: String,
    ) {
        _uiState.update {
            when(state.type) {
                JusulsaUiState.JusulsaType.AddOn -> it.copy(
                    addOnState = state.copy(
                        image = screen,
                        texts = listOf(text)
                    )
                )
                JusulsaUiState.JusulsaType.Result -> it.copy(
                    resultState = state.copy(
                        image = screen,
                        texts = listOf(text)
                    )
                )
            }
        }
    }

    private suspend fun updateFromLocal(
        state: JusulsaUiState.State,
        duration: Duration,
    ) = withContext(Dispatchers.IO) {
        while (isActive) {
            if(actionJob?.isActive == true) {
                val screen = DisplayProvider.capture2(state.rectangle)
                val text = TextDetecter.detectString(screen)
                updateScreen(state, screen, text)
            }
            delay(duration)
        }
    }

    private fun execute(block: suspend CoroutineScope.() -> Unit) {
        actionJob?.cancel()
        scope.launch { block() }
    }

    override fun dispatch(event: UiEvent): Job { throw IllegalAccessException("not implementation") }
    fun dispatchKeyPressEvent(keyEvent: Int) = scope.launch {}
}