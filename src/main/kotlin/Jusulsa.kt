import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import common.event.RegisterNativeHook
import common.presentation.applyOpacity
import jusulsa.JusulsaApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "주술사",
        state = rememberWindowState(
            size = DpSize(400.dp, 460.dp),
            position = WindowPosition.PlatformDefault,
        ),
        alwaysOnTop = true,
    ) {
        // 게임 화면 위에 떠 있으므로 반투명하게 해서 뒤가 보이게 한다
        var opacity by remember { mutableFloatStateOf(1f) }
        LaunchedEffect(opacity) { window.applyOpacity(opacity) }

        RegisterNativeHook()
        JusulsaApp(
            opacity = opacity,
            onOpacityChange = { opacity = it },
        )
    }
}
