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
    private val _rectangle = MutableStateFlow(FollowerUiState.default.magicResultRect)
    val rectangle = _rectangle.asStateFlow()

    private val tesseract = Tesseract().apply {
        setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata")
        setLanguage("kor")
    }

    fun updateRectangle(rect: Rectangle) {
        _rectangle.value = rect
    }

    suspend fun detect(targets: List<String>): Boolean = withContext(Dispatchers.IO) {
        delay(100)
        val rectangle = rectangle.value
        val image = Keyboard.capture(rectangle)
        val origin = tesseract.doOCR(image.toAwtImage())
        print(origin)
        targets.forEach {
            if(origin.contains(it)) {
                println("-> 실패!!")
                return@withContext true
            }
        }
        return@withContext false
    }

    suspend fun detectString(): String = withContext(Dispatchers.IO) {
        delay(100)
        val rectangle = rectangle.value
        val image = Keyboard.capture(rectangle)
        val origin = tesseract.doOCR(image.toAwtImage())
        return@withContext origin
    }
}