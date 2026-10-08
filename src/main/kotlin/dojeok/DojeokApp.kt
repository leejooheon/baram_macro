package dojeok

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import common.robot.KeyHook
import common.robot.Keyboard
import kotlinx.coroutines.*
import java.awt.event.KeyEvent

/**
 * 도적용 경량 매크로.
 * - F2: 스페이스 연타(100ms에 1번) 켜기/끄기
 * - ` 누르면 8, / 누르면 , 를 대신 보낸다(원래 키는 게임에 안 넘어간다). 꾹 누르면 그대로 꾹 누른 것처럼 동작한다.
 */
object DojeokMacro {
    private const val SPACE_INTERVAL = 100L

    // 사용자 키(VC) -> 대신 보낼 키(AWT VK)
    private val remap = mapOf(
        NativeKeyEvent.VC_BACKQUOTE to KeyEvent.VK_8,
        NativeKeyEvent.VC_SLASH to KeyEvent.VK_COMMA,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var spaceJob: Job? = null

    var isRunning by mutableStateOf(false)
        private set

    private val listener = object : KeyHook.Listener {
        override val consumeKeys = remap.keys + NativeKeyEvent.VC_F2

        override fun onKey(keyCode: Int, pressed: Boolean) {
            remap[keyCode]?.let { target ->
                if (pressed) Keyboard.press(target) else Keyboard.release(target)
                return
            }
            if (keyCode == NativeKeyEvent.VC_F2 && pressed) toggle()
        }
    }

    fun start() {
        if (KeyHook.isAvailable) KeyHook.addListener(listener)
    }

    @Synchronized
    fun toggle() {
        if (spaceJob?.isActive == true) {
            spaceJob?.cancel()
            spaceJob = null
            isRunning = false
            return
        }
        isRunning = true
        spaceJob = scope.launch {
            var next = System.currentTimeMillis()
            while (isActive) {
                Keyboard.pressAndRelease(KeyEvent.VK_SPACE)
                next += SPACE_INTERVAL
                delay((next - System.currentTimeMillis()).coerceAtLeast(0))
            }
        }
    }
}

@Composable
fun DojeokApp() {
    LaunchedEffect(Unit) { DojeokMacro.start() }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val running = DojeokMacro.isRunning
            Text(
                text = if (running) "스페이스 연타 중 (F2 정지)" else "정지 (F2 시작)",
                color = if (running) Color(0xFF2E7D32) else Color.Gray,
                style = MaterialTheme.typography.subtitle1,
            )
            Text("` → 8", style = MaterialTheme.typography.body2)
            Text("/ → ,", style = MaterialTheme.typography.body2)
        }
    }
}
