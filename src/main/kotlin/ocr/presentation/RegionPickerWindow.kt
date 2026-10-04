package ocr.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import ocr.model.TimerRegion
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 전체 화면 캡처를 깔고 마우스로 드래그해서 영역을 고르는 창.
 * 창이 화면 전체를 덮으므로 (픽셀 / 배율) 이 곧 Robot 좌표다.
 */
@Composable
fun RegionPickerWindow(
    region: TimerRegion,
    screenshot: BufferedImage,
    current: Rectangle,
    onConfirm: (Rectangle) -> Unit,
    onCancel: () -> Unit,
) {
    var selection by remember { mutableStateOf<Rect?>(null) }
    val density = remember { screenScale() }

    fun confirm() {
        val rect = selection ?: return
        onConfirm(rect.toAwtRectangle(density))
    }

    Window(
        onCloseRequest = onCancel,
        title = "${region.title} 영역 지정",
        state = rememberWindowState(placement = WindowPlacement.Fullscreen),
        undecorated = true,
        alwaysOnTop = true,
        onKeyEvent = { event ->
            if (event.type != KeyEventType.KeyUp) return@Window false
            when (event.key) {
                Key.Escape -> { onCancel(); true }
                Key.Enter -> { confirm(); true }
                else -> false
            }
        },
    ) {
        val bitmap = remember(screenshot) { screenshot.toComposeImageBitmap() }

        Box(Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var start = Offset.Zero
                        detectDragGestures(
                            onDragStart = { start = it; selection = Rect(it, it) },
                            onDrag = { change, _ ->
                                val end = change.position
                                selection = Rect(
                                    left = min(start.x, end.x),
                                    top = min(start.y, end.y),
                                    right = maxOf(start.x, end.x),
                                    bottom = maxOf(start.y, end.y),
                                ).takeIf { abs(start.x - end.x) > 2 && abs(start.y - end.y) > 2 }
                            },
                        )
                    }
            ) {
                drawImage(
                    image = bitmap,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    dstOffset = IntOffset.Zero,
                )
                drawRect(Color.Black.copy(alpha = 0.35f))
                // 지금 쓰고 있는 영역 (점선)
                drawRect(
                    color = Color.Yellow,
                    topLeft = Offset(current.x * density, current.y * density),
                    size = androidx.compose.ui.geometry.Size(current.width * density, current.height * density),
                    style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))),
                )
                selection?.let {
                    drawRect(color = Color.Red, topLeft = it.topLeft, size = it.size, style = Stroke(width = 3f))
                }
            }

            Card(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
                backgroundColor = Color(0xEE222222),
                contentColor = Color.White,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val hint = selection?.toAwtRectangle(density)?.let { "(${it.x}, ${it.y})  ${it.width}×${it.height}" }
                        ?: "${region.title}이(가) 보이는 곳을 드래그하세요. 노란 점선은 지금 영역이에요."
                    Text(hint)
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = ::confirm, enabled = selection != null) { Text("저장 (Enter)") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onCancel) { Text("취소 (Esc)") }
                }
            }
        }
    }
}

/** 윈도우 배율 (150%면 1.5). Compose의 density와 같은 값이다. */
private fun screenScale(): Float = GraphicsEnvironment.getLocalGraphicsEnvironment()
    .defaultScreenDevice.defaultConfiguration.defaultTransform.scaleX.toFloat()

private fun Rect.toAwtRectangle(density: Float): Rectangle {
    return Rectangle(
        (left / density).roundToInt(),
        (top / density).roundToInt(),
        (width / density).roundToInt(),
        (height / density).roundToInt(),
    )
}
