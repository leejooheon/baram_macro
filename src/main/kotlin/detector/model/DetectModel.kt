package detector.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** OCR 서버 `/detect/` 응답 */
@Serializable
data class DetectModel(
    @SerialName("objects") val objects: List<DetectedObjectModel>,
    @SerialName("elapsed_ms") val elapsedMs: Double,
)

@Serializable
data class DetectedObjectModel(
    /** monster 또는 me */
    @SerialName("label") val label: String,
    @SerialName("confidence") val confidence: Double,
    /** 보낸 이미지(게임 창) 픽셀 기준 [x, y, w, h] */
    @SerialName("box") val box: List<Int>,
)
