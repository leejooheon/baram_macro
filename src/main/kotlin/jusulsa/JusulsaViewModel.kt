package jusulsa

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import common.base.BaseViewModel
import common.model.MoveEvent
import common.model.UiEvent
import common.robot.Keyboard
import common.util.onError
import common.util.onSuccess
import follower.macro.FollowerMacro
import follower.macro.MacroDetailAction
import follower.ocr.TextDetecter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.awt.event.KeyEvent
import kotlin.time.Duration.Companion.seconds

class JusulsaViewModel: BaseViewModel() {
    private val scope = CoroutineScope(SupervisorJob())
    private var job: Job? = null
    private val macroDetailAction = MacroDetailAction()
    private val kingHelper = KingHelper(scope)
    private val moveHelper = MoveHelper()

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
        when(keyEvent) {
            NativeKeyEvent.VC_ESCAPE -> {
                job?.cancel()
                kingHelper.cancel()
            }
            NativeKeyEvent.VC_BACKQUOTE -> macroDetailAction.healMe()
            NativeKeyEvent.VC_F1 -> kingHelper.start()
            NativeKeyEvent.VC_F2 -> {
                start(KeyEvent.VK_4)
//                moveHelper.goToKing()
            }
            NativeKeyEvent.VC_F3 -> {
                start(KeyEvent.VK_5)
//                moveHelper.goToDungeon()
            }
            NativeKeyEvent.VC_W -> {
                job?.cancel()
            }
        }
    }

    fun dispatchKeyPressEvent(keyEvent: Int) = scope.launch {

    }

    private fun start(keyEvent: Int) {
        job?.cancel()
        job = scope.launch {
            macroDetailAction.loop(keyEvent)
        }
    }

    private fun observeScreens() = scope.launch {
//        launch(Dispatchers.IO) {
//            while (isActive) {
//                updateCoordinates(
//                    duration = (0.1).seconds,
//                    action = {
////                        moveHelper.goToDungeon()
////                        FollowerMacro.dispatch(MoveEvent.OnMove)
//                    }
//                )
//            }
//        }
    }

    override fun dispatch(event: UiEvent): Job {
        TODO("Not yet implemented")
    }
}