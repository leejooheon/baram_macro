package follower.ocr

import androidx.compose.ui.graphics.toAwtImage
import common.Keyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.sourceforge.tess4j.Tesseract
import java.awt.Rectangle
import java.awt.Toolkit

class FailureDetecter {
    private val _rectangle = MutableStateFlow(Rectangle(Toolkit.getDefaultToolkit().screenSize))
    val rectangle = _rectangle.asStateFlow()

// 1100, 760, 400, 60
    private val tesseract = Tesseract().apply {
        setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata")
        setLanguage("kor")
    }

    fun updateRectangle(rect: Rectangle) {
        _rectangle.value = rect
    }

    suspend fun detect(): Boolean = withContext(Dispatchers.IO) {
        val rectangle = rectangle.value
        val image = Keyboard.capture(rectangle)
        val origin = tesseract.doOCR(image.toAwtImage())
        println(origin)
        return@withContext origin.contains("실패")
    }
}