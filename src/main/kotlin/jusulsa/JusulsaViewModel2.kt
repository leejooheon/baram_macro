package jusulsa

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import common.network.OcrClient
import common.network.createHttpClient
import common.robot.DisplayProvider
import common.robot.Keyboard
import common.robot.UserInput
import common.util.Result
import io.ktor.client.plugins.logging.LogLevel
import jusulsa.model.JusulsaUiState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import ocr.TimerMonitor
import java.awt.Rectangle
import java.awt.event.KeyEvent
import kotlin.time.Duration.Companion.seconds

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
    private val ocrClient = OcrClient(createHttpClient(logLevel = LogLevel.NONE))
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
        UserInput.onKey(keyEvent, pressed = false)

        when(keyEvent) {
            NativeKeyEvent.VC_BACKQUOTE -> scope.launch { Keyboard.pressAndRelease(KeyEvent.VK_8) }
            NativeKeyEvent.VC_PAGE_UP -> execute { macroDetailAction.chumChum() }

            // 개편 중이라 나머지 단축키는 잠시 꺼둔다
//            NativeKeyEvent.VC_PAGE_UP -> hellfire()
            NativeKeyEvent.VC_PAGE_DOWN -> actionJob?.cancel()
//            NativeKeyEvent.VC_SLASH -> execute { macroDetailAction.mabeAroundMe() }
//            NativeKeyEvent.VC_BACK_SLASH -> execute { macroDetailAction.maagi() }
//            NativeKeyEvent.VC_KANJI -> execute { macroDetailAction.jeoju() }
//            NativeKeyEvent.VC_1 -> execute { macroDetailAction.heal() }
//            NativeKeyEvent.VC_2 -> execute { macroDetailAction.mabee() }
//            NativeKeyEvent.VC_3 -> execute { macroDetailAction.julmang() }
            NativeKeyEvent.VC_F1 -> execute { macroDetailAction.hondon() }

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

    /**
     * 테스트 탭: 게임 창을 앞으로 가져온 뒤 기능 하나만 실행한다.
     * 게임 창을 못 찾았으면 그 사이에 게임 창을 직접 누를 수 있게 3초 기다린다.
     */
    fun test(name: String, block: suspend MacroDetailAction2.() -> Unit) {
        execute {
            val focused = TimerMonitor.focusGameWindow()
            setTestStatus(if (focused) "$name 실행 중" else "$name: 3초 안에 게임 창을 눌러 주세요")
            delay(if (focused) TEST_FOCUS_MILLIS else TEST_MANUAL_FOCUS_MILLIS)
            setTestStatus("$name 실행 중")
            try {
                macroDetailAction.block()
                setTestStatus("$name 끝")
            } catch (e: CancellationException) {
                setTestStatus("$name 멈춤")
                throw e
            }
        }
    }

    /** 테스트 탭의 정지 버튼. 멈출 때 보내는 키(넘버락 등)가 게임에 가도록 게임 창을 먼저 앞으로 가져온다 */
    fun stop() {
        TimerMonitor.focusGameWindow()
        actionJob?.cancel()
    }

    private fun setTestStatus(status: String) {
        _uiState.update { it.copy(testStatus = status) }
    }

    private fun observeScreens() = scope.launch {
        hellfireCount.collectLatest {
            _uiState.update { state -> state.copy(count = it) }
        }
    }

    /** 헬파이어가 나갔는지 쿨타임 박스를 OCR 서버로 읽어 확인한다 */
    private suspend fun checkHellfireDelay(): Boolean {
        delay(200)
        repeat(2) {
            val screen = DisplayProvider.capture(COOLDOWN_RECT)
            val lines = (ocrClient.readTimers(screen) as? Result.Success)?.data?.lines.orEmpty()
            if (lines.any { "헬" in it.name || "헬" in it.raw }) return true
        }
        return false
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
        /** 게임 창을 앞으로 가져온 뒤 키를 보내기까지 기다리는 시간 */
        private const val TEST_FOCUS_MILLIS = 500L
        private const val TEST_MANUAL_FOCUS_MILLIS = 3_000L

        /** 화면 우상단 스킬 쿨타임 박스 */
        private val COOLDOWN_RECT = Rectangle(1267, 60, 170, 120)


        // 매크로 단축키는 게임에 넘기지 않는다. 방향키와 ESC는 게임에서도 써야 하므로 뺀다
        internal val MACRO_KEYS = setOf(
            NativeKeyEvent.VC_BACKQUOTE,
            NativeKeyEvent.VC_PAGE_UP,
        )
    }
    fun dispatchKeyPressEvent(keyEvent: Int) {
        UserInput.onKey(keyEvent, pressed = true)
    }
}