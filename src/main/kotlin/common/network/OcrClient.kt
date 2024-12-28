package common.network

import androidx.compose.ui.graphics.ImageBitmap
import common.model.OcrModel
import common.util.NetworkError
import common.util.Result
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*
import javax.imageio.ImageIO

class OcrClient(
    private val httpClient: HttpClient
) {
    suspend fun readImage(
        bufferedImage: BufferedImage
    ): Result<OcrModel, NetworkError> = withContext(Dispatchers.IO) {
        val fileName = UUID.randomUUID().toString()
        val file = File(fileName)
        val response = try {
            ImageIO.write(bufferedImage, "png", file)

            httpClient.submitFormWithBinaryData(
                url = "http://$host:$ocrPort/ocr/",
                formData = formData {
                    append(
                        "file",
                        file.readBytes(),
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$fileName.png\"")
                        }
                    )
                }
            )
        } catch(e: UnresolvedAddressException) {
            return@withContext Result.Error(NetworkError.NO_INTERNET)
        } catch(e: SerializationException) {
            return@withContext Result.Error(NetworkError.SERIALIZATION)
        } finally {
            file.delete()
        }

        return@withContext when(response.status.value) {
            in 200..299 -> {
                val model = response.body<OcrModel>()
                Result.Success(model)
            }
            401 -> Result.Error(NetworkError.UNAUTHORIZED)
            409 -> Result.Error(NetworkError.CONFLICT)
            408 -> Result.Error(NetworkError.REQUEST_TIMEOUT)
            413 -> Result.Error(NetworkError.PAYLOAD_TOO_LARGE)
            in 500..599 -> Result.Error(NetworkError.SERVER_ERROR)
            else -> Result.Error(NetworkError.UNKNOWN)
        }
    }
}