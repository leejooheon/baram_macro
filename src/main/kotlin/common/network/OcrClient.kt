package common.network

import common.util.NetworkError
import common.util.Result
import detector.model.DetectModel
import ocr.model.OcrHealthModel
import ocr.model.TimerOcrModel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class OcrClient(
    private val httpClient: HttpClient
) {
    /** 쿨타임 박스나 버프 패널 캡처를 보내 '이름 N초' 줄 목록을 받는다. 임시 파일 없이 메모리에서 바로 보낸다. */
    suspend fun readTimers(
        bufferedImage: BufferedImage
    ): Result<TimerOcrModel, NetworkError> = upload("ocr/timers/", bufferedImage, "png")

    /**
     * 게임 창 전체 캡처를 보내 몹/내 캐릭터 박스를 받는다. 창 전체라 PNG는 인코딩이 느려서 JPEG로 보낸다.
     * 학습한 모델이 없으면 서버가 503을 준다 ([NetworkError.SERVER_ERROR]).
     */
    suspend fun detect(
        bufferedImage: BufferedImage
    ): Result<DetectModel, NetworkError> = upload("detect/", bufferedImage, "jpg")

    private suspend inline fun <reified T> upload(
        path: String,
        image: BufferedImage,
        format: String,
    ): Result<T, NetworkError> = withContext(Dispatchers.IO) {
        val bytes = ByteArrayOutputStream().use {
            ImageIO.write(image, format, it)
            it.toByteArray()
        }
        val contentType = if (format == "png") "image/png" else "image/jpeg"
        val response = try {
            httpClient.submitFormWithBinaryData(
                url = "http://$host:$ocrPort/$path",
                formData = formData {
                    append(
                        "file",
                        bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, contentType)
                            append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"image.$format\"")
                        }
                    )
                }
            )
        } catch (e: Exception) {
            return@withContext Result.Error(NetworkError.REQUEST_TIMEOUT)
        }

        return@withContext when(val status = response.status.value) {
            in 200..299 -> Result.Success(response.body<T>())
            else -> parseError(status)
        }
    }

    suspend fun health(): Result<OcrHealthModel, NetworkError> = withContext(Dispatchers.IO) {
        val response = try {
            httpClient.get(urlString = "http://$host:$ocrPort/health/")
        } catch (e: Exception) {
            return@withContext Result.Error(NetworkError.REQUEST_TIMEOUT)
        }

        return@withContext when(val status = response.status.value) {
            in 200..299 -> Result.Success(response.body<OcrHealthModel>())
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