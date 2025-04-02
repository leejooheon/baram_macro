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
            size = DpSize(450.dp, 450.dp),
            position = WindowPosition(1267.dp, 260.dp) // 화면 상단 좌측 고정
        ),
        alwaysOnTop = true,
        undecorated = true, // ✅ 반드시 필요: 투명 윈도우 설정 시 테두리 제거
        transparent = true  // ✅ 창을 투명하게 설정
    ) {
        RegisterNativeHook()
        JusulsaApp()
    }
}