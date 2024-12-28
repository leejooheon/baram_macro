package common.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class EventModel(val keyEvent: Int, val isPressed: Boolean) {
    override fun toString(): String {
        return Json.encodeToString(serializer(), this)
    }
    companion object {
        fun String.toEventModel(): EventModel? {
            return try {
                Json.decodeFromString(serializer(), this)
            } catch (e: Exception) {
                null
            }
        }
    }
}