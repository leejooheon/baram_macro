package common.model.api

import common.model.PositionResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PositionModel(
    @SerialName("result") val result: PositionResult,
)