import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import commander.CommanderApp
import common.event.RegisterNativeHook
import jusulsa.JusulsaApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(
            size = DpSize(450.dp, 180.dp),
            position = WindowPosition(1267.dp, 260.dp),
//            position = WindowPosition(30.dp, 630.dp)
        ),
        alwaysOnTop = true,
        undecorated = true, // ✅ 반드시 필요: 투명 윈도우 설정 시 테두리 제거
        transparent = true  // ✅ 창을 투명하게 설정
    ) {
        RegisterNativeHook()
        JusulsaApp()
    }
}