package common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@Serializable
data class EventModel(val keyEvent: Int, val isPressed: Boolean) {
    companion object {
        fun String.toModel(): EventModel {
            return Json.decodeFromString(serializer(), this)
        }
    }
}