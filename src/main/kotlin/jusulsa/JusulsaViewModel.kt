package jusulsa

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import common.base.BaseViewModel
import common.model.UiEvent
import common.robot.DisplayProvider
import common.robot.Keyboard
import follower.macro.MacroDetailAction
import follower.macro.MacroDetailAction.Companion.CHUM1
import follower.macro.MacroDetailAction.Companion.GONGJEUNG
import follower.macro.MacroDetailAction.Companion.HEAL
import follower.macro.MacroDetailAction.Companion.HELLFIRE
import follower.macro.MacroDetailAction.Companion.JEOJU
import follower.model.MagicResultState
import follower.ocr.TextDetecter
import jusulsa.model.JusulsaUiState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import java.awt.Point
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class JusulsaViewModel: BaseViewModel() {
    private val scope = CoroutineScope(SupervisorJob())
    private var job: Job? = null
    private var job2: Job? = null
    private val macroDetailAction = MacroDetailAction()
    private val kingHelper = KingHelper(scope)
    private val moveHelper = MoveHelper()
    private var bomuTime = 0L

    private val _uiState = MutableStateFlow(JusulsaUiState.default)
    internal val uiState = _uiState.asStateFlow()
    private val cnt = AtomicInteger(0)
    init {
        scope.launch {
            launch(Dispatchers.IO) {
                kingHelper.currentState.collectLatest {
                    when(it) {
                        KingHelper.State.MISSION_ACCEPT -> moveHelper.goToDungeon()
                        else -> { /** nothing **/ }
                    }
                }
            }
        }
        observeScreens()
    }

    private var latestDirection: Int = KeyEvent.VK_LEFT
    fun dispatchKeyReleaseEvent(keyEvent: Int) = scope.launch {
//        println("dispatch:$keyEvent")
        when(keyEvent) {
            NativeKeyEvent.VC_UP,
            NativeKeyEvent.VC_LEFT,
            NativeKeyEvent.VC_DOWN,
            NativeKeyEvent.VC_RIGHT -> {
                latestDirection = when(keyEvent) {
                    NativeKeyEvent.VC_UP -> KeyEvent.VK_UP
                    NativeKeyEvent.VC_LEFT -> KeyEvent.VK_LEFT
                    NativeKeyEvent.VC_DOWN -> KeyEvent.VK_DOWN
                    NativeKeyEvent.VC_RIGHT -> KeyEvent.VK_RIGHT
                    else -> KeyEvent.VK_LEFT
                }
            }
            NativeKeyEvent.VC_PAGE_DOWN -> {
                job?.cancel()
                job2?.cancel()
                kingHelper.cancel()
                Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
                Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
            }
            NativeKeyEvent.VC_F1 -> {
                job?.cancel()
                job = scope.launch(Dispatchers.IO) {
                    heal()
//                    chumchum()
                }
            }
            NativeKeyEvent.VC_F4 -> {
                job?.cancel()
                job = scope.launch(Dispatchers.IO) {
                    chumchum()
                    macroDetailAction.test()
                }
            }
            NativeKeyEvent.VC_F2 -> {
                job?.cancel()
                job = scope.launch(Dispatchers.IO) {
                    try {
                        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
                        delay(65)
                        macroDetailAction.mabee()
                    } catch (e: Exception) {
                        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
                        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
                    }
                }
            }

            NativeKeyEvent.VC_PAGE_UP -> {
//                job?.cancel()
//                job = scope.launch(Dispatchers.IO) {
//                    macroDetailAction.tabTab()
//                    delay(65)
//                    while (isActive) {
//                        Keyboard.pressAndRelease(HEAL)
//                        delay(320)
//                    }
//                }
                cnt.getAndIncrement()
                updateState()
                if (job?.isActive == true) return@launch
                else cnt.set(1)
                updateState()
                job = try {
                    scope.launch(Dispatchers.IO) {
                        while (cnt.get() > 0) {
                            cnt.getAndDecrement()
                            updateState()
                            hellfire()
                        }
                    }
                } catch (e: Exception) {
                    Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
                    null
                }
            }

            NativeKeyEvent.VC_KANJI -> {
                job?.cancel()
                job = scope.launch {
                    try {
                        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
                        delay(65)
                        macroDetailAction.julmang()
                    } catch (e: Exception) {
                        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
                    }
                }
            }
            NativeKeyEvent.VC_BACKQUOTE -> {
                val duration = 20L
                Keyboard.press(KeyEvent.VK_SHIFT)
                delay(duration)
                Keyboard.pressAndRelease(KeyEvent.VK_Z)
                delay(duration)
                Keyboard.pressAndRelease(KeyEvent.VK_C)
                Keyboard.release(KeyEvent.VK_SHIFT)
            }
            NativeKeyEvent.VC_SLASH -> {
                Keyboard.press(KeyEvent.VK_SHIFT)
                delay(20)
                Keyboard.pressAndRelease(KeyEvent.VK_COMMA)
                Keyboard.release(KeyEvent.VK_SHIFT)
            }

            3638 -> {
                job?.cancel()
                job = scope.launch(Dispatchers.IO) {
                    Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
                    delay(20)
                    Keyboard.pressAndRelease(KeyEvent.VK_U)
                    delay(20)
                    Keyboard.pressAndRelease(KeyEvent.VK_U)
                    tryGongjeung()
                    hellfire()
                }
            }
        }
    }

    fun dispatchMouse(point: Point, button: Int) {

    }

    fun dispatchKeyPressEvent(keyEvent: Int) = scope.launch {

    }

    private fun observeScreens() = scope.launch {
        launch {
            updateFromLocal2(
                state = uiState.value.addOnState,
                duration = 1.seconds
            )
        }
//        launch {
//            updateFromLocal2(
//                state = uiState.value.resultState,
//                duration = 1.seconds
//            )
//        }
    }

    override fun dispatch(event: UiEvent): Job {
        TODO("Not yet implemented")
    }

    private suspend fun hellfire() {
        Keyboard.pressAndRelease(JEOJU)
        Keyboard.pressAndRelease(KeyEvent.VK_HOME, 50)
        Keyboard.pressAndRelease(latestDirection)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
        Keyboard.pressAndRelease(HELLFIRE)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
        val startTime = System.currentTimeMillis()
        if (checkDelay()) {
            tryGongjeung()
            val consumedTime = System.currentTimeMillis() - startTime
            macroDetailAction.tabTab()
            withTimeoutOrNull(7.seconds.inWholeMilliseconds - consumedTime) {
                heal()
            }
            delay(65)
            Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        }
    }
    private suspend fun chumchum() = withContext(Dispatchers.IO) {
//        launch {
//            while (isActive) {
//                Keyboard.pressAndRelease(KeyEvent.VK_A)
//                delay(140)
//            }
//        }

        macroDetailAction.bomuMe()
//        launch {
//            while (isActive) {
//                withTimeoutOrNull(5.seconds) {
//                    macroDetailAction.jeoju()
//                }
//                withTimeoutOrNull(30.seconds) {
//                    macroDetailAction.jungdok()
//                }
//                delay(60)
//                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
//            }
//        }
//        launch {
//            while (isActive) {
//                val duration = 20L
//                Keyboard.press(KeyEvent.VK_SHIFT)
//                delay(duration)
//                Keyboard.pressAndRelease(KeyEvent.VK_Z)
//                Keyboard.release(KeyEvent.VK_SHIFT)
//                delay(duration)
//                Keyboard.pressAndRelease(KeyEvent.VK_L)
//                delay(400)
//
//                Keyboard.press(KeyEvent.VK_SHIFT)
//                delay(duration)
//                Keyboard.pressAndRelease(KeyEvent.VK_Z)
//                Keyboard.release(KeyEvent.VK_SHIFT)
//                delay(duration)
//                Keyboard.pressAndRelease(KeyEvent.VK_M)
//                delay(400)
//        }
//    }
}

private suspend fun checkDelay(): Boolean = withContext(Dispatchers.IO) {
    delay(200)
    repeat(2) {
        val screen = DisplayProvider.capture2(uiState.value.addOnState.rectangle)
        val text = TextDetecter.detectString(screen)
        updateScreen(uiState.value.addOnState, screen, text)
        if (text.contains("헬")) return@withContext true
    }
    return@withContext false
}

private suspend fun tryGongjeung() = withContext(Dispatchers.IO) {
    while (isActive) {
        val startTime = System.currentTimeMillis()
        Keyboard.pressAndRelease(GONGJEUNG)
        delay(200)
        val screen = DisplayProvider.capture2(uiState.value.resultState.rectangle)

        val healAsync = async {
            Keyboard.pressAndRelease(HEAL)
            Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
        }
        val textAsync =  async {
            TextDetecter.detectString(screen)
        }
        val text = listOf(textAsync, healAsync).awaitAll().firstOrNull() as? String ?: return@withContext

        updateScreen(uiState.value.resultState, screen, text)
        when {
            text.contains("곰력") -> break
            text.contains(MagicResultState.GONGJEUNG.tag) -> break
        }
        val duration = (startTime + 500L) - System.currentTimeMillis()
        println("duration: $duration: $text")
        if(duration > 0) delay(duration)
    }
}

    private suspend fun heal() = withContext(Dispatchers.IO) {
        repeat(3) {
            Keyboard.pressAndRelease(HEAL)
            if(it == 0) Keyboard.pressAndRelease(KeyEvent.VK_HOME)
            Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
            delay(40)
        }
        if(System.currentTimeMillis() > bomuTime + 150.seconds.inWholeMilliseconds) {
            macroDetailAction.bomuMe()
            bomuTime = System.currentTimeMillis()
        }
        Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        macroDetailAction.tabTab()
        delay(50)
        while (isActive) {
            Keyboard.pressAndRelease(HEAL)
            delay(70)
        }
    }

    private suspend fun updateFromLocal2(
        state: JusulsaUiState.State,
        duration: Duration,
    ) = withContext(Dispatchers.IO) {
        while (isActive) {
            if(job?.isActive == true) {
                val screen = DisplayProvider.capture2(state.rectangle)
                val text = TextDetecter.detectString(screen)
                updateScreen(state, screen, text)
            }
            delay(duration)
        }
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

    private fun updateState() {
        _uiState.value = _uiState.value.copy(count = cnt.get())
    }
}