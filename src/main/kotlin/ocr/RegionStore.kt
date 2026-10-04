package ocr

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ocr.model.TimerRegion
import java.awt.geom.Rectangle2D
import java.io.File

/** 게임 창 제목과 영역(게임 창 대비 비율)을 `~/.baram_macro/ocr_settings.json`에 저장한다. */
object RegionStore {
    const val DEFAULT_WINDOW_KEYWORD = "옛날바람"

    private val file = File(System.getProperty("user.home"), ".baram_macro/ocr_settings.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Serializable
    private data class Fraction(val x: Double, val y: Double, val width: Double, val height: Double)

    @Serializable
    private data class Settings(
        val windowKeyword: String = DEFAULT_WINDOW_KEYWORD,
        val regions: Map<String, Fraction> = emptyMap(),
    )

    data class Loaded(
        val windowKeyword: String,
        val regions: Map<TimerRegion, Rectangle2D.Double>,
    )

    fun load(): Loaded {
        val settings = runCatching { json.decodeFromString<Settings>(file.readText()) }.getOrDefault(Settings())
        return Loaded(
            windowKeyword = settings.windowKeyword,
            regions = TimerRegion.entries.associateWith { region ->
                settings.regions[region.name]
                    ?.let { Rectangle2D.Double(it.x, it.y, it.width, it.height) }
                    ?: region.defaultFraction
            }
        )
    }

    fun save(windowKeyword: String, regions: Map<TimerRegion, Rectangle2D.Double>) {
        runCatching {
            file.parentFile.mkdirs()
            val settings = Settings(
                windowKeyword = windowKeyword,
                regions = regions.entries.associate { (region, r) ->
                    region.name to Fraction(r.x, r.y, r.width, r.height)
                }
            )
            file.writeText(json.encodeToString(settings))
        }
    }
}
