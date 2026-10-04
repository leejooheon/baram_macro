package common.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import common.robot.KeyHook
import common.robot.WinInput

/**
 * 전역 키 입력을 받는다. 키 코드는 jnativehook의 VC_* 값.
 *
 * Windows에서는 직접 건 훅(KeyHook)을 써서 매크로가 보낸 키는 무시하고,
 * consumeKeys에 있는 키는 게임에 넘기지 않는다. 그 외 OS에서는 jnativehook을 쓴다(consumeKeys 미지원).
 */
@Composable
fun ObserveKeyEvents(
    onReleased: (Int) -> Unit,
    onPressed: (Int) -> Unit,
    consumeKeys: Set<Int> = emptySet(),
) {
    DisposableEffect(Unit) {
        if (WinInput.isAvailable) {
            val listener = object : KeyHook.Listener {
                override val consumeKeys = consumeKeys
                override fun onKey(keyCode: Int, pressed: Boolean) {
                    if (pressed) onPressed(keyCode) else onReleased(keyCode)
                }
            }
            KeyHook.addListener(listener)
            onDispose { KeyHook.removeListener(listener) }
        } else {
            val keyListener = object : NativeKeyListener {
                override fun nativeKeyReleased(nativeEvent: NativeKeyEvent?) {
                    nativeEvent?.let { onReleased(it.keyCode) }
                }

                override fun nativeKeyPressed(nativeEvent: NativeKeyEvent?) {
                    nativeEvent?.let { onPressed(it.keyCode) }
                }
            }
            GlobalScreen.addNativeKeyListener(keyListener)
            onDispose { GlobalScreen.removeNativeKeyListener(keyListener) }
        }
    }
}
