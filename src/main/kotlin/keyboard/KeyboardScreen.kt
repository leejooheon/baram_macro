package keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import keyboard.model.KeyboardItem

@Composable
internal fun KeyboardScreen() {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color(0x88000000)) // ✅ 반투명 검은색 배경
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(end = 50.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
        ) {

//            Column {
//                KeyboardButton(
//                    item = KeyboardItem.MABEE,
//                    modifier = Modifier
//                )
//                KeyboardButton(
//                    item = KeyboardItem.HEAL,
//                    modifier = Modifier
//                )
//            }
            Column {
                KeyboardButton(
                    item = KeyboardItem.PageUp,
                    modifier = Modifier
                )
                KeyboardButton(
                    item = KeyboardItem.Left,
                    modifier = Modifier
                )
            }
            Column {
                KeyboardButton(
                    item = KeyboardItem.Up,
                    modifier = Modifier
                )
                KeyboardButton(
                    item = KeyboardItem.Down,
                    modifier = Modifier
                )
            }
            Column {
                KeyboardButton(
                    item = KeyboardItem.PageDown,
                    modifier = Modifier
                )
                KeyboardButton(
                    item = KeyboardItem.Right,
                    modifier = Modifier
                )
            }
            Column {
                KeyboardButton(
                    item = KeyboardItem.MABEE,
                    modifier = Modifier
                )
                KeyboardButton(
                    item = KeyboardItem.HEAL,
                    modifier = Modifier
                )
            }
        }
    }
}