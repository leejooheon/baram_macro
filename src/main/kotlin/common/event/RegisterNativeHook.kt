package common.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.kwhat.jnativehook.GlobalScreen

@Composable
fun RegisterNativeHook() {
    DisposableEffect(Unit) {
        GlobalScreen.registerNativeHook()

        onDispose {
            GlobalScreen.unregisterNativeHook()
        }
    }
}