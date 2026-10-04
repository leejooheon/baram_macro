package detector

import java.awt.Point
import java.awt.Rectangle
import kotlin.math.roundToInt

/** 게임 칸 좌표. 내 캐릭터가 (0, 0) */
data class Tile(val x: Int, val y: Int) {
    val neighbors: List<Tile> get() = listOf(Tile(x, y - 1), Tile(x, y + 1), Tile(x - 1, y), Tile(x + 1, y))
    fun isNeighborOf(other: Tile) = other in neighbors
}

/**
 * 탐지 박스를 칸 좌표로 바꾼다. 바람은 칸 단위로 움직이고 캐릭터와 몹이 한 칸 크기라,
 * 내 캐릭터 박스 크기(없으면 몹 박스 크기의 중간값)를 한 칸으로 본다.
 */
class TileGrid(private val origin: Point, private val tileWidth: Double, private val tileHeight: Double) {
    fun tileOf(box: Rectangle): Tile {
        val c = box.center()
        return Tile(((c.x - origin.x) / tileWidth).roundToInt(), ((c.y - origin.y) / tileHeight).roundToInt())
    }

    companion object {
        fun of(detection: Detection): TileGrid? {
            val size = detection.me ?: detection.monsters.sortedBy { it.height }.getOrNull(detection.monsters.size / 2)
            if (size == null || size.width <= 0 || size.height <= 0) return null
            return TileGrid(detection.myPosition, size.width.toDouble(), size.height.toDouble())
        }
    }
}
