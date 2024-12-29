package common.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class KeyEventModel(val keyEvent: Int) {
    override fun toString(): String {
        return Json.encodeToString(serializer(), this)
    }
    companion object {
        fun String.toKeyEventModel(): KeyEventModel? {
            return try {
                Json.decodeFromString(serializer(), this)
            } catch (e: Exception) {
                null
            }
        }
    }
}