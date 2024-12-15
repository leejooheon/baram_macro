package follower.ocr

import androidx.compose.ui.graphics.toAwtImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.sourceforge.tess4j.Tesseract
import java.awt.Rectangle
import java.awt.Toolkit

class TextDetecter(
    private val target: String,
) {
    private val _rectangle = MutableStateFlow(Rectangle(Toolkit.getDefaultToolkit().screenSize))
    val rectangle = _rectangle.asStateFlow()

    private val tesseract = Tesseract().apply {
        setDatapath("/opt/homebrew/Cellar/tesseract/5.4.1/share/tessdata")
//        setLanguage("kor")
    }
    fun updateRectangle(rect: Rectangle) {
        _rectangle.value = rect
    }
    suspend fun start() = withContext(Dispatchers.IO) {
        while (true) {
//            val image = MyRobot.capture(rectangle.value)
//            val origin = tesseract.doOCR(image.toAwtImage())
//            println(origin)
        }
    }
}