package follower.ocr

import androidx.compose.ui.graphics.toAwtImage
import common.Keyboard
import follower.model.FollowerUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.sourceforge.tess4j.Tesseract
import java.awt.Rectangle

class TextDetecter {

    private val tesseract = Tesseract().apply {
        setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata")
        setLanguage("kor")
    }

    suspend fun detect(
        targets: List<String>,
        rectangle: Rectangle
    ): Boolean = withContext(Dispatchers.IO) {
        delay(100)
        val image = Keyboard.capture(rectangle)
        try {
            val origin = tesseract.doOCR(image.toAwtImage())
            return@withContext targets.contains(origin)
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun detectString(
        rectangle: Rectangle
    ): String = withContext(Dispatchers.IO) {
        delay(100)
        val image = Keyboard.capture(rectangle)
        val origin = tesseract.doOCR(image.toAwtImage())
        return@withContext origin
    }
}