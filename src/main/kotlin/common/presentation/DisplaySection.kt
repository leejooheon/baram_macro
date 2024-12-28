package common.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    Column(
        modifier = Modifier
    ) {
        Row {
            Image(
                bitmap = model.image.toComposeImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = model.texts.joinToString(separator = "\n"),
                style = MaterialTheme.typography.body1.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.widthIn(32.dp)
            )
        }

        SizeItem(
            rectangle = model.rectangle,
            onRectangleChanged = onRectangleChanged
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}