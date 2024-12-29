package common.presentation

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.awt.Rectangle
import javax.swing.tree.DefaultTreeCellEditor.DefaultTextField

@Preview
@Composable
internal fun SizeItem(
    rectangle: Rectangle,
    onRectangleChanged: (Rectangle) -> Unit,
) {
    Row {
        CustomTextField(
            value = rectangle.x.toString(),
            label = "x 좌표",
            contentDescription = "x",
            onValueChanged = {
                val rect = Rectangle(it, rectangle.y, rectangle.width, rectangle.height)
                onRectangleChanged.invoke(rect)
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        CustomTextField(
            value = rectangle.y.toString(),
            label = "y 좌표",
            contentDescription = "y",
            onValueChanged = {
                val rect = Rectangle(rectangle.x, it, rectangle.width, rectangle.height)
                onRectangleChanged.invoke(rect)
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        CustomTextField(
            value = rectangle.width.toString(),
            label = "width",
            contentDescription = "width",
            onValueChanged = {
                val rect = Rectangle(rectangle.x, rectangle.y, it, rectangle.height)
                onRectangleChanged.invoke(rect)
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        CustomTextField(
            value = rectangle.height.toString(),
            label = "height",
            contentDescription = "height",
            onValueChanged = {
                val rect = Rectangle(rectangle.x, rectangle.y, rectangle.width, it)
                onRectangleChanged.invoke(rect)
            }
        )
    }
}

@Composable
private fun CustomTextField(
    value: String,
    label: String,
    contentDescription: String,
    onValueChanged: (Int) -> Unit
) {
    OutlinedTextField(
        value = value,
        textStyle = MaterialTheme.typography.body2,
        singleLine = true,
        label = { Text(text = label) },
        placeholder = { Text(text = contentDescription) },
        onValueChange = {
            it.toIntOrNull()?.let {
                onValueChanged.invoke(it)
            }
        },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color(0xFFF5F5F5),
            focusedIndicatorColor = Color.Blue,
            focusedLabelColor = Color.Blue,
            unfocusedLabelColor = Color.Gray,
            unfocusedIndicatorColor = Color.Gray,
            cursorColor = Color.Red,
        ),
        modifier = Modifier.width(96.dp)
    )
}