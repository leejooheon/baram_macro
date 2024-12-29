package follower.ocr

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import common.robot.Keyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.sourceforge.tess4j.Tesseract
import java.awt.Rectangle
import java.awt.image.BufferedImage

object TextDetecter {
    private val tesseract = Tesseract().apply {
        setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata")
        setLanguage("kor")
    }

    private val mutex = Mutex()

    suspend fun detectString(
        image: BufferedImage,
    ): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                tesseract.doOCR(image)
            } catch (e: Exception) {
                ""
            }
        }
    }
}