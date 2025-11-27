package jusulsa.model

import common.presentation.SizeItem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.awt.Rectangle
import java.awt.image.BufferedImage

@Composable
fun JusulsaDisplaySection(
    texts: List<String>,
    image: BufferedImage,
    rectangle: Rectangle,
    onRectangleChanged: (Rectangle) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.clip(MaterialTheme.shapes.medium)) {
                Image(
                    bitmap = image.toComposeImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { isExpanded = !isExpanded }
            ) {
                Icon(
                    imageVector = if(isExpanded) Icons.Outlined.KeyboardArrowDown
                    else Icons.Outlined.KeyboardArrowUp,
                    contentDescription = if(isExpanded) "down" else "up"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = texts.joinToString(separator = "\n"),
                colors = TextFieldDefaults.textFieldColors(
//                    backgroundColor = Color(0xFFF5F5F5),
                    disabledTextColor = Color.White,
                ),
                textStyle = MaterialTheme.typography.body1.copy(
                    fontWeight = FontWeight.Bold
                ),
                enabled = false,
                maxLines = 3,
                onValueChange = {},
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            SizeItem(
                rectangle = rectangle,
                onRectangleChanged = onRectangleChanged
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

//@Preview
//@Composable
//private fun PreviewDisplaySection() {
//    MaterialTheme {
//        JusulsaDisplaySection(
//            model = UiState.CommonState.default,
//            onRectangleChanged = {},
//            modifier = Modifier
//                .width(128.dp)
//                .height(64.dp)
//        )
//    }
//}
