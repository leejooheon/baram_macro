package common.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PointModel(
    val x: Int,
    val y: Int,
) {
    override fun toString(): String {
        return Json.encodeToString(serializer(), this)
    }
    companion object {
        fun String.toPointModel(): PointModel? {
            return try {
                Json.decodeFromString(serializer(), this)
            } catch (e: Exception) {
                null
            }
        }
    }
}