package common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class EventModel(val keyEvent: Int, val isPressed: Boolean) {
    override fun toString(): String {
        return Json.encodeToString(serializer(), this)
    }
    companion object {
        fun String.toModel(): EventModel {
            return Json.decodeFromString(serializer(), this)
        }
    }
}