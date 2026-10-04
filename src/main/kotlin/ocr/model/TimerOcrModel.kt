package ocr.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** OCR 서버 `/ocr/timers/` 응답 */
@Serializable
data class TimerOcrModel(
    @SerialName("lines") val lines: List<TimerLineModel>,
    @SerialName("elapsed_ms") val elapsedMs: Double,
    @SerialName("cached") val cached: Boolean,
)

@Serializable
data class TimerLineModel(
    /** 알려진 이름으로 보정된 이름 (예: 호체주술) */
    @SerialName("name") val name: String,
    /** 인식한 그대로의 글자 (예: 호주술 49초) */
    @SerialName("raw") val raw: String,
    /** 'N초'의 N. 숫자를 못 읽으면 null */
    @SerialName("seconds") val seconds: Int? = null,
    @SerialName("confidence") val confidence: Double,
    /** 캡처 이미지 픽셀 기준 [x, y, w, h] */
    @SerialName("box") val box: List<Int>,
)

/** OCR 서버 `/health/` 응답 */
@Serializable
data class OcrHealthModel(
    @SerialName("gpu") val gpu: Boolean,
    @SerialName("threads") val threads: Int,
    @SerialName("names") val names: List<String> = emptyList(),
)
