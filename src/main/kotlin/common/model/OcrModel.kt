package common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OcrModel(
    @SerialName("result") val results: List<String>,
    @SerialName("confidence") val confidence: Double,
)