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
 * - F3: 1 연타(100ms에 1번) 켜기/끄기. 둘 중 하나만 돈다(다른 쪽을 누르면 그쪽으로 바뀐다)
 * - 켜져 있는 동안만 ` 누르면 8, / 누르면 , 를 대신 보낸다(원래 키는 게임에 안 넘어간다). 꾹 누르면 그대로 꾹 누른 것처럼 동작한다.
 */
object DojeokMacro {
    private const val INTERVAL = 100L

    // 단축키(VC) -> 연타할 키(AWT VK)
    private val toggleKeys = mapOf(
        NativeKeyEvent.VC_F2 to KeyEvent.VK_SPACE,
        NativeKeyEvent.VC_F3 to KeyEvent.VK_1,
    )

    // 사용자 키(VC) -> 대신 보낼 키(AWT VK)
    private val remap = mapOf(
        NativeKeyEvent.VC_BACKQUOTE to KeyEvent.VK_8,
        NativeKeyEvent.VC_SLASH to KeyEvent.VK_COMMA,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var repeatJob: Job? = null

    /** 지금 연타 중인 키(AWT VK). 꺼져 있으면 null */
    var repeatingKey by mutableStateOf<Int?>(null)
        private set

    val isRunning get() = repeatingKey != null

    private val listener = object : KeyHook.Listener {
        // 꺼져 있을 땐 ` 와 / 를 그대로 게임에 넘긴다
        override val consumeKeys: Set<Int>
            get() = if (isRunning) remap.keys + toggleKeys.keys else toggleKeys.keys

        override fun onKey(keyCode: Int, pressed: Boolean) {
            remap[keyCode]?.let { target ->
                // 끈 뒤에 뗀 키는 눌린 채로 남지 않게 항상 떼 준다
                if (pressed && isRunning) Keyboard.press(target) else if (!pressed) Keyboard.release(target)
                return
            }
            if (pressed) toggleKeys[keyCode]?.let { toggle(it) }
        }
    }

    fun start() {
        if (KeyHook.isAvailable) KeyHook.addListener(listener)
    }

    /** 같은 키면 끄고, 다른 키면 그 키 연타로 바꾼다 */
    @Synchronized
    fun toggle(key: Int) {
        val wasRunning = repeatingKey
        repeatJob?.cancel()
        repeatJob = null
        repeatingKey = null
        if (wasRunning == key) return

        repeatingKey = key
        repeatJob = scope.launch {
            var next = System.currentTimeMillis()
            while (isActive) {
                Keyboard.pressAndRelease(key)
                next += INTERVAL
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
            val key = DojeokMacro.repeatingKey
            Text(
                text = when (key) {
                    KeyEvent.VK_SPACE -> "스페이스 연타 중 (F2 정지)"
                    KeyEvent.VK_1 -> "1 연타 중 (F3 정지)"
                    else -> "정지 (F2 스페이스, F3 1)"
                },
                color = if (key != null) Color(0xFF2E7D32) else Color.Gray,
                style = MaterialTheme.typography.subtitle1,
            )
            Text("` → 8", style = MaterialTheme.typography.body2)
            Text("/ → ,", style = MaterialTheme.typography.body2)
        }
    }
}
