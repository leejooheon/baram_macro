package common.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import common.robot.InjectedKeys

@Composable
fun ObserveKeyEvents(
    onReleased: (Int) -> Unit,
    onPressed: (Int) -> Unit,
) {
    DisposableEffect(Unit) {
        val keyListener = object : NativeKeyListener {
            override fun nativeKeyReleased(nativeEvent: NativeKeyEvent?) {
                val event = nativeEvent ?: return
                // 매크로가 보낸 키는 단축키로 처리하지 않는다
                if (InjectedKeys.consume(event.rawCode, pressed = false)) return
                onReleased.invoke(event.keyCode)
            }

            override fun nativeKeyPressed(nativeEvent: NativeKeyEvent?) {
                val event = nativeEvent ?: return
                if (InjectedKeys.consume(event.rawCode, pressed = true)) return
                onPressed.invoke(event.keyCode)
            }
        }

        GlobalScreen.addNativeKeyListener(keyListener)
        onDispose {
            GlobalScreen.removeNativeKeyListener(keyListener)
        }
    }
}
