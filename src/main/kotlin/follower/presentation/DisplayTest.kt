package follower.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import java.awt.Rectangle
import java.awt.Toolkit

@Composable
internal fun DisplayTest(
    rectangle: Rectangle,
    onRectangleChange: (Rectangle) -> Unit,
) {
//    var rectangle by remember { mutableStateOf(Rectangle(Toolkit.getDefaultToolkit().screenSize)) }
//    val image = MyRobot.capture(rectangle)

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.weight(0.65f).background(Color.Black)) {
//            Image(
//                bitmap = MyRobot.capture(rectangle),
//                contentDescription = null,
//                contentScale = ContentScale.None,
////                modifier = Modifier.aspectRatio(16f / 9f)
//            )
        }
        Box(modifier = Modifier.weight(0.35f)) {
            Column {
                BasicTextField(
                    value = rectangle.x.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let {
                            onRectangleChange.invoke(
                                Rectangle(it, rectangle.y, rectangle.width, rectangle.height)
                            )
                        }
                    },
                )
                BasicTextField(
                    value = rectangle.y.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let {
                            onRectangleChange.invoke(
                                Rectangle(rectangle.x, it, rectangle.width, rectangle.height)
                            )
                        }
                    },
                )
                BasicTextField(
                    value = rectangle.width.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let {
                            onRectangleChange.invoke(
                                Rectangle(rectangle.x, rectangle.y, it, rectangle.height)
                            )
                        }
                    },
                )
                BasicTextField(
                    value = rectangle.height.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let {
                            onRectangleChange.invoke(
                                Rectangle(rectangle.x, rectangle.y, rectangle.width, it)
                            )
                        }
                    },
                )
            }
        }
    }

}