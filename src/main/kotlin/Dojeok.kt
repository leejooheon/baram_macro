import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import dojeok.DojeokApp

// 실행: gradlew run -PmainClass=DojeokKt
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "도적",
        state = rememberWindowState(
            size = DpSize(240.dp, 160.dp),
            position = WindowPosition.PlatformDefault,
        ),
        alwaysOnTop = true,
    ) {
        DojeokApp()
    }
}
