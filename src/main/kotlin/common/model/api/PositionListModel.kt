package common.model.api

import common.model.PositionResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PositionListModel(
    @SerialName("result") val result: List<PositionResult>,
)