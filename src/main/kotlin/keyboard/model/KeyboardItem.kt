package keyboard.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.awt.event.KeyEvent

enum class KeyboardItem(
    val event: Int,
    val icon: ImageVector,
) {
    Up(
        event = KeyEvent.VK_UP,
        icon = Icons.Default.KeyboardArrowUp,
    ),
    Down(
        event = KeyEvent.VK_DOWN,
        icon = Icons.Default.KeyboardArrowDown,
    ),
    Left(
        event = KeyEvent.VK_LEFT,
        icon = Icons.Default.KeyboardArrowLeft,
    ),
    Right(
        event = KeyEvent.VK_RIGHT,
        icon = Icons.Default.KeyboardArrowRight,
    ),
    PageUp(
        event = KeyEvent.VK_PAGE_UP,
        icon = Icons.Default.PlayArrow,
    ),
    PageDown(
        event = KeyEvent.VK_PAGE_DOWN,
        icon = Icons.Default.ArrowDropDown,
    ),
    ;
}