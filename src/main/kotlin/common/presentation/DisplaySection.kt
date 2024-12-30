package common.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.desktop.ui.tooling.preview.Preview
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
import common.model.UiState
import java.awt.Rectangle

@Composable
internal fun DisplaySection(
    model: UiState.CommonState,
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
                    bitmap = model.image.toComposeImageBitmap(),
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
                value = model.texts.joinToString(separator = "\n"),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color(0xFFF5F5F5),
                    disabledTextColor = Color.Black
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
                rectangle = model.rectangle,
                onRectangleChanged = onRectangleChanged
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview
@Composable
private fun PreviewDisplaySection() {
    MaterialTheme {
        DisplaySection(
            model = UiState.CommonState.default,
            onRectangleChanged = {},
            modifier = Modifier
                .width(128.dp)
                .height(64.dp)
        )
    }
}
