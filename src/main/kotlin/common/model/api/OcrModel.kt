package common.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OcrModel(
    @SerialName("result") val results: List<String>,
)