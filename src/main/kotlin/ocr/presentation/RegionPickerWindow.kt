package ocr.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import ocr.model.TimerRegion
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 게임 창 캡처를 띄워 두고 마우스로 드래그해서 영역을 고르는 창.
 * 결과는 게임 창 대비 비율이라 창 위치나 크기가 바뀌어도 그대로 쓸 수 있다.
 */
@Composable
fun RegionPickerWindow(
    region: TimerRegion,
    frame: BufferedImage,
    regions: Map<TimerRegion, Rectangle2D.Double>,
    onConfirm: (Rectangle2D.Double) -> Unit,
    onCancel: () -> Unit,
) {
    var selection by remember { mutableStateOf<Rectangle2D.Double?>(null) }

    fun confirm() {
        selection?.let(onConfirm)
    }

    Window(
        onCloseRequest = onCancel,
        title = "${region.title} 영역 지정",
        state = rememberWindowState(size = DpSize(1280.dp, 860.dp), position = WindowPosition.PlatformDefault),
        onKeyEvent = { event ->
            if (event.type != KeyEventType.KeyUp) return@Window false
            when (event.key) {
                Key.Escape -> { onCancel(); true }
                Key.Enter -> { confirm(); true }
                else -> false
            }
        },
    ) {
        val bitmap = remember(frame) { frame.toComposeImageBitmap() }

        Column(Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val hint = selection?.let {
                    val px = it.x * frame.width
                    val py = it.y * frame.height
                    "(${px.roundToInt()}, ${py.roundToInt()})  " +
                        "${(it.width * frame.width).roundToInt()}×${(it.height * frame.height).roundToInt()}"
                } ?: region.pickHint
                Text(hint, modifier = Modifier.weight(1f))
                Button(onClick = ::confirm, enabled = selection != null) { Text("저장 (Enter)") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onCancel) { Text("취소 (Esc)") }
            }

            // 이미지를 비율 유지로 맞춘 위치. 드래그 좌표를 비율로 바꿀 때 쓴다
            var imageRect by remember { mutableStateOf(Rect.Zero) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var start = Offset.Zero
                        detectDragGestures(
                            onDragStart = { start = it },
                            onDrag = { change, _ ->
                                val end = change.position
                                if (abs(start.x - end.x) < 3 || abs(start.y - end.y) < 3) return@detectDragGestures
                                val r = imageRect
                                fun fx(v: Float) = ((v - r.left) / r.width).toDouble().coerceIn(0.0, 1.0)
                                fun fy(v: Float) = ((v - r.top) / r.height).toDouble().coerceIn(0.0, 1.0)
                                val x0 = fx(min(start.x, end.x))
                                val y0 = fy(min(start.y, end.y))
                                selection = Rectangle2D.Double(
                                    x0, y0,
                                    fx(maxOf(start.x, end.x)) - x0,
                                    fy(maxOf(start.y, end.y)) - y0,
                                )
                            },
                        )
                    }
            ) {
                val scale = min(size.width / frame.width, size.height / frame.height)
                val drawSize = Size(frame.width * scale, frame.height * scale)
                val topLeft = Offset((size.width - drawSize.width) / 2, (size.height - drawSize.height) / 2)
                imageRect = Rect(topLeft, drawSize)

                drawImage(
                    image = bitmap,
                    dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
                    dstSize = IntSize(drawSize.width.roundToInt(), drawSize.height.roundToInt()),
                )

                fun Rectangle2D.Double.toRect() = Rect(
                    offset = Offset(topLeft.x + (x * drawSize.width).toFloat(), topLeft.y + (y * drawSize.height).toFloat()),
                    size = Size((width * drawSize.width).toFloat(), (height * drawSize.height).toFloat()),
                )

                regions.forEach { (other, fraction) ->
                    val r = fraction.toRect()
                    drawRect(
                        color = if (other == region) Color.Yellow else Color.Cyan.copy(alpha = 0.6f),
                        topLeft = r.topLeft,
                        size = r.size,
                        style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))),
                    )
                }
                selection?.toRect()?.let {
                    drawRect(color = Color.Red, topLeft = it.topLeft, size = it.size, style = Stroke(width = 3f))
                }
            }
        }
    }
}
