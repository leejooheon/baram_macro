package jusulsa

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import common.base.BaseViewModel
import common.model.UiEvent
import common.model.UiState.Type
import common.robot.DisplayProvider
import common.robot.Keyboard
import follower.macro.MacroDetailAction
import follower.model.MagicResultState
import follower.ocr.TextDetecter
import jusulsa.model.JusulsaUiState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import java.awt.Point
import java.awt.Rectangle
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


    fun dispatchKeyReleaseEvent(keyEvent: Int) = scope.launch {
        println("dispatch:$keyEvent")
        when(keyEvent) {
            NativeKeyEvent.VC_PAGE_DOWN -> {
                job?.cancel()
                job2?.cancel()
                kingHelper.cancel()
                Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
            }
            NativeKeyEvent.VC_F1 -> {
                job?.cancel()
                job = scope.launch(Dispatchers.IO) {
                    macroDetailAction.bomuMe()
                    launch {
                        while (isActive) {
                            Keyboard.pressAndRelease(KeyEvent.VK_A)
                            delay(140)
                        }
                    }
                    launch {
                        while (isActive) {
                            withTimeoutOrNull(5.seconds) {
                                macroDetailAction.honmasul()
                            }
                            withTimeoutOrNull(30.seconds) {
                                macroDetailAction.julmang()
                            }
                            delay(60)
                            Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
                        }
                    }
                    launch {
                        while (isActive) {
                            Keyboard.pressAndRelease(KeyEvent.VK_5)
                            delay(400)
                            Keyboard.pressAndRelease(KeyEvent.VK_6)
                            delay(520)
                        }
                    }
                }
            }

            NativeKeyEvent.VC_F2 -> {
                job?.cancel()
                job = scope.launch(Dispatchers.IO) {
                    macroDetailAction.tabTab()
                    delay(65)
                    while (isActive) {
                        Keyboard.pressAndRelease(KeyEvent.VK_3)
                        delay(333)
                    }
                }
            }

            NativeKeyEvent.VC_PAGE_UP -> {
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
                job = scope.launch(Dispatchers.IO) {
                    while (isActive) {
                        withTimeoutOrNull(1.seconds) {
                            macroDetailAction.honmasul()
                        }
                    }
                }
            }
            NativeKeyEvent.VC_BACKQUOTE -> {
                Keyboard.pressAndRelease(KeyEvent.VK_0)
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
                    delay(20)
                    Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
                    delay(60)
                    Keyboard.pressAndRelease(KeyEvent.VK_4)
                    delay(60)
                    Keyboard.mouseClick()
                    Keyboard.pressAndRelease(KeyEvent.VK_ENTER)

                    delay(20)
                    Keyboard.pressAndRelease(KeyEvent.VK_U)
                    delay(20)
                    Keyboard.pressAndRelease(KeyEvent.VK_U)
                    tryGongjeung()
                    hellfire()
                    }
            }
            null -> {

            }
        }
    }

    fun dispatchMouse(point: Point, button: Int) {
        if(button == 2) {
            job?.cancel()
            job = scope.launch(Dispatchers.IO) {
                delay(20)
                Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
                delay(60)
                Keyboard.pressAndRelease(KeyEvent.VK_4)
                delay(60)
                Keyboard.mouseClick()
                Keyboard.pressAndRelease(KeyEvent.VK_ENTER)

                delay(20)
                Keyboard.pressAndRelease(KeyEvent.VK_U)
                delay(20)
                Keyboard.pressAndRelease(KeyEvent.VK_U)
                tryGongjeung()
//                hellfire()
                Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
                dispatchKeyReleaseEvent(NativeKeyEvent.VC_F1)
            }
        }
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
        Keyboard.pressAndRelease(KeyEvent.VK_1)
        Keyboard.pressAndRelease(KeyEvent.VK_LEFT)
        Keyboard.pressAndRelease(KeyEvent.VK_ENTER)
        val startTime = System.currentTimeMillis()
        if (checkDelay()) {
            tryGongjeung()
            macroDetailAction.tabTab()
            val consumedTime = System.currentTimeMillis() - startTime
            withTimeoutOrNull(7.seconds.inWholeMilliseconds - consumedTime) {
                while (isActive) {
                    Keyboard.pressAndRelease(KeyEvent.VK_3)
                    delay(300)
                }
            }
            delay(65)
            Keyboard.pressAndRelease(KeyEvent.VK_ESCAPE)
        }
    }

    private suspend fun checkDelay(): Boolean = withContext(Dispatchers.IO) {
        delay(250)
        val screen = DisplayProvider.capture2(uiState.value.addOnState.rectangle)
        val text = TextDetecter.detectString(screen)
        updateScreen(uiState.value.addOnState, screen, text)
        return@withContext text.contains("헬")
    }

    private suspend fun tryGongjeung() = withContext(Dispatchers.IO) {
        while (isActive) {
            Keyboard.pressAndRelease(KeyEvent.VK_2)
            delay(150)
            val screen = DisplayProvider.capture2(uiState.value.resultState.rectangle)
            val text = TextDetecter.detectString(screen)

            updateScreen(uiState.value.resultState, screen, text)
            when {
                text.contains(MagicResultState.GONGJEUNG.tag) -> break
            }
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