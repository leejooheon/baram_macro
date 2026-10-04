package jusulsa

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import common.base.BaseViewModel
import common.model.UiEvent
import common.robot.DisplayProvider
import common.robot.Keyboard
import common.robot.WindowKeyboard
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
    private val macroDetailAction = MacroDetailAction2()
    @Volatile private var actionJob: Job? = null
    @Volatile private var hellfireJob: Job? = null
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

            // 임시: PostMessage 키 전달 테스트. 원래는 혼돈(hondon)
            NativeKeyEvent.VC_F1 -> execute {
                val window = WindowKeyboard.foregroundWindow() ?: return@execute
                println("PostMessage 보무 테스트: ${WindowKeyboard.describe(window)}")
                macroDetailAction.bomuByPostMessage(window)
            }
            NativeKeyEvent.VC_F4 -> execute { macroDetailAction.chumChum() }

            NativeKeyEvent.VC_UP -> macroDetailAction.onDirectionChanged(KeyEvent.VK_UP)
            NativeKeyEvent.VC_LEFT -> macroDetailAction.onDirectionChanged(KeyEvent.VK_LEFT)
            NativeKeyEvent.VC_DOWN -> macroDetailAction.onDirectionChanged(KeyEvent.VK_DOWN)
            NativeKeyEvent.VC_RIGHT -> macroDetailAction.onDirectionChanged(KeyEvent.VK_RIGHT)

            NativeKeyEvent.VC_ESCAPE -> actionJob?.cancel()
        }
    }

    private fun hellfire() {
        // 헬파이어가 이미 돌고 있으면 횟수만 쌓는다. 다른 매크로가 돌고 있을 땐 그걸 멈추고 헬파이어를 시작한다
        if (hellfireJob?.isActive == true) {
            hellfireCount.update { it + 1 }
            return
        }
        hellfireCount.value = 1

        hellfireJob = execute {
            while (hellfireCount.value > 0) {
                hellfireCount.update { it - 1 }
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
                _uiState.update { state -> state.copy(count = it) }
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

    override fun dispatch(event: UiEvent): Job { throw IllegalAccessException("not implementation") }

    companion object {
        // 매크로 단축키는 게임에 넘기지 않는다. 방향키와 ESC는 게임에서도 써야 하므로 뺀다
        internal val MACRO_KEYS = setOf(
            NativeKeyEvent.VC_PAGE_UP,
            NativeKeyEvent.VC_PAGE_DOWN,
            NativeKeyEvent.VC_BACKQUOTE,
            NativeKeyEvent.VC_SLASH,
            NativeKeyEvent.VC_BACK_SLASH,
            NativeKeyEvent.VC_KANJI,
            NativeKeyEvent.VC_1,
            NativeKeyEvent.VC_2,
            NativeKeyEvent.VC_3,
            NativeKeyEvent.VC_F1,
            NativeKeyEvent.VC_F4,
        )
    }
    fun dispatchKeyPressEvent(keyEvent: Int) = scope.launch {}
}