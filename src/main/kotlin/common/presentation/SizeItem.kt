package common.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import java.awt.Rectangle


@Composable
internal fun SizeItem(
    rectangle: Rectangle,
    onRectangleChanged: (Rectangle) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Text(
            text = "X: ",
            style = MaterialTheme.typography.body2
        )
        BasicTextField(
            value = rectangle.x.toString(),
            textStyle = MaterialTheme.typography.body2,
            onValueChange = {
                it.toIntOrNull()?.let {
                    onRectangleChanged.invoke(
                        Rectangle(it, rectangle.y, rectangle.width, rectangle.height)
                    )
                }
            },
        )

        Text(
            text = "Y: ",
            style = MaterialTheme.typography.body2
        )
        BasicTextField(
            value = rectangle.y.toString(),
            textStyle = MaterialTheme.typography.body2,
            onValueChange = {
                it.toIntOrNull()?.let {
                    onRectangleChanged.invoke(
                        Rectangle(rectangle.x, it, rectangle.width, rectangle.height)
                    )
                }
            },
        )

        Text(
            text = "Width: ",
            style = MaterialTheme.typography.body2
        )
        BasicTextField(
            value = rectangle.width.toString(),
            textStyle = MaterialTheme.typography.body2,
            onValueChange = {
                it.toIntOrNull()?.let {
                    onRectangleChanged.invoke(
                        Rectangle(rectangle.x, rectangle.y, it, rectangle.height)
                    )
                }
            },
        )

        Text(
            text = "Height: ",
            style = MaterialTheme.typography.body2
        )
        BasicTextField(
            value = rectangle.height.toString(),
            textStyle = MaterialTheme.typography.body2,
            onValueChange = {
                it.toIntOrNull()?.let {
                    onRectangleChanged.invoke(
                        Rectangle(rectangle.x, rectangle.y, rectangle.width, it)
                    )
                }
            },
        )
    }
}