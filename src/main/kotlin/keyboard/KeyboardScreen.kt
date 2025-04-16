package keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import keyboard.model.KeyboardItem

@Composable
internal fun KeyboardScreen() {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color(0x88000000)) // ✅ 반투명 검은색 배경
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .background(Color.Blue),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            KeyboardButton(
                item = KeyboardItem.Up,
                modifier = Modifier
            )
            Row {
                KeyboardButton(
                    item = KeyboardItem.Left,
                    modifier = Modifier
                )
                KeyboardButton(
                    item = KeyboardItem.Down,
                    modifier = Modifier
                )
                KeyboardButton(
                    item = KeyboardItem.Right,
                    modifier = Modifier
                )
            }
        }
    }
}