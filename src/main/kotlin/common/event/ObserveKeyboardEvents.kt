package common.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener

@Composable
fun ObserveKeyEvents(
    onReleased: (Int) -> Unit,
    onPressed: (Int) -> Unit,
) {
    DisposableEffect(Unit) {
        val keyListener = object : NativeKeyListener {
            override fun nativeKeyReleased(nativeEvent: NativeKeyEvent?) {
                super.nativeKeyPressed(nativeEvent)
                nativeEvent?.keyCode?.let { keyCode ->
                    onReleased.invoke(keyCode)
                }
            }

            override fun nativeKeyPressed(nativeEvent: NativeKeyEvent?) {
                super.nativeKeyPressed(nativeEvent)

                nativeEvent?.keyCode?.let { keyCode ->
                    onPressed.invoke(keyCode)
//                    println("Pressed $keyCode")
                }
            }
        }

        GlobalScreen.addNativeKeyListener(keyListener)
        onDispose {
            GlobalScreen.removeNativeKeyListener(keyListener)
        }
    }
}