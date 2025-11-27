package keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import common.robot.Keyboard
import keyboard.model.KeyboardItem

@Composable
fun KeyboardButton(
    item: KeyboardItem,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }
    LaunchedEffect(isPressed) {
        println("isPressed: $isPressed")
    }
    Box(
        modifier = modifier
            .size(192.dp)
            .shadow(8.dp, shape = RoundedCornerShape(12.dp), clip = false)
            .background(
                color = if (isPressed) Color(0xFFDDDDDD).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        Keyboard.press(item.event)
                        tryAwaitRelease()
                        isPressed = false
                        Keyboard.release(item.event)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = Color.Black, // 필요하면 색상 조절
            modifier = Modifier.size(48.dp)
        )
    }
}