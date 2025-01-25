package follower.ocr

import common.network.OcrClient
import common.network.createHttpClient
import common.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.sourceforge.tess4j.Tesseract
import java.awt.image.BufferedImage

object TextDetecter {
    private val tesseract = Tesseract().apply {
        setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata")
        setLanguage("kor")
    }
    private val ocrClient = OcrClient(createHttpClient())
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

    suspend fun detectStringRemote(
        image: BufferedImage,
    ): String = withContext(Dispatchers.IO) {
        return@withContext when(val result = ocrClient.readImage(image)) {
            is Result.Success -> result.data.results.joinToString("\n")
            is Result.Error -> ""
        }
    }

    suspend fun detectStringRemoteRaw(
        image: BufferedImage
    ) = ocrClient.readImage(image)
}