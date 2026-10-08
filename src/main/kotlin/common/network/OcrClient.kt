package common.network

import common.util.NetworkError
import common.util.Result
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
    ): Result<TimerOcrModel, NetworkError> = withContext(Dispatchers.IO) {
        ImageIO.setUseCache(false) // 임시 파일 대신 메모리를 쓰도록 설정 (디스크 I/O 병목 및 NPE 방지)
        val bytes = ByteArrayOutputStream().use {
            ImageIO.write(bufferedImage, "png", it)
            it.toByteArray()
        }
        val response = try {
            httpClient.submitFormWithBinaryData(
                url = "http://$host:$ocrPort/ocr/timers/",
                formData = formData {
                    append(
                        "file",
                        bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"region.png\"")
                        }
                    )
                }
            )
        } catch (e: Exception) {
            return@withContext Result.Error(NetworkError.REQUEST_TIMEOUT)
        }

        return@withContext when(val status = response.status.value) {
            in 200..299 -> {
                try {
                    Result.Success(response.body<TimerOcrModel>())
                } catch (e: Exception) {
                    // 가끔 서버 응답이 꼬여서 JsonArray 등 엉뚱한 포맷이 날아와도 앱이 죽지 않게 방어
                    Result.Error(NetworkError.SERVER_ERROR)
                }
            }
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