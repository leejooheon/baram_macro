package common.network

import common.model.api.OcrModel
import common.model.api.PositionListModel
import common.model.api.PositionModel
import common.util.NetworkError
import common.util.Result
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
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
        } catch (e: Exception) {
            file.delete()
            return@withContext Result.Error(NetworkError.REQUEST_TIMEOUT)
        }
        finally {
            file.delete()
        }

        return@withContext when(val status = response.status.value) {
            in 200..299 -> {
                val model = response.body<OcrModel>()
                Result.Success(model)
            }
            else -> parseError(status)
        }
    }

    suspend fun findKing(): Result<PositionModel, NetworkError> = withContext(Dispatchers.IO) {
        val response = try {
            httpClient.get(
                url = URL("http://$host:$ocrPort/find/king"),
            )
        } catch (e: Exception) {
            return@withContext Result.Error(NetworkError.REQUEST_TIMEOUT)
        }

        return@withContext when(val status = response.status.value) {
            in 200..299 -> {
                val model = response.body<PositionModel>()
                Result.Success(model)
            }
            else -> parseError(status)
        }
    }


    suspend fun conversationWithKing(): Result<PositionListModel, NetworkError> = withContext(Dispatchers.IO) {
        val response = try {
            httpClient.get(
                url = URL("http://$host:$ocrPort/conversation/king"),
            )
        } catch (e: Exception) {
            return@withContext Result.Error(NetworkError.REQUEST_TIMEOUT)
        }

        return@withContext when(val status = response.status.value) {
            in 200..299 -> {
                val model = response.body<PositionListModel>()
                Result.Success(model)
            }
            else -> parseError(status)
        }
    }

    private fun parseError(status: Int) = when(status) {
        401 -> Result.Error(NetworkError.UNAUTHORIZED)
        409 -> Result.Error(NetworkError.CONFLICT)
        408 -> Result.Error(NetworkError.REQUEST_TIMEOUT)
        413 -> Result.Error(NetworkError.PAYLOAD_TOO_LARGE)
        in 500..599 -> Result.Error(NetworkError.SERVER_ERROR)
        else -> Result.Error(NetworkError.UNKNOWN)
    }
}