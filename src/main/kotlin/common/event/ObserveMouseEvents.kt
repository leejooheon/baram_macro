package common.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener
import java.awt.Point

@Composable
fun ObserveMouseEvents(
    onClicked: (point: Point) -> Unit
) {

    DisposableEffect(Unit) {
        val listener = object : NativeMouseInputListener {
            override fun nativeMouseClicked(nativeEvent: NativeMouseEvent?) {
                super.nativeMouseClicked(nativeEvent)
                val event = nativeEvent ?: return

//                println("nativeMouseClicked: ${event.point}, ${event.button}, ${event.clickCount}")
                onClicked.invoke(event.point)
            }
        }
        GlobalScreen.addNativeMouseListener(listener)

        onDispose {
            GlobalScreen.removeNativeMouseListener(listener)
        }
    }
}