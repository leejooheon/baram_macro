package ocr

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ocr.model.TimerRegion
import java.awt.Rectangle
import java.io.File

/** 영역 좌표를 `~/.baram_macro/ocr_regions.json`에 저장해서 다음 실행 때 다시 입력하지 않게 한다. */
object RegionStore {
    private val file = File(System.getProperty("user.home"), ".baram_macro/ocr_regions.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Serializable
    private data class SavedRectangle(val x: Int, val y: Int, val width: Int, val height: Int)

    fun load(): Map<TimerRegion, Rectangle> {
        val saved = runCatching {
            json.decodeFromString<Map<String, SavedRectangle>>(file.readText())
        }.getOrDefault(emptyMap())

        return TimerRegion.entries.associateWith { region ->
            saved[region.name]
                ?.let { Rectangle(it.x, it.y, it.width, it.height) }
                ?: region.defaultRectangle
        }
    }

    fun save(regions: Map<TimerRegion, Rectangle>) {
        runCatching {
            file.parentFile.mkdirs()
            val data = regions.entries.associate { (region, rect) ->
                region.name to SavedRectangle(rect.x, rect.y, rect.width, rect.height)
            }
            file.writeText(json.encodeToString(data))
        }
    }
}
