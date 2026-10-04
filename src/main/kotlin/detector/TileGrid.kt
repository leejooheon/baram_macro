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
 * 탐지 박스를 칸 좌표로 바꾼다. 바람의 칸은 정사각형이고 너비가 캐릭터 박스 너비와 거의 같다.
 * 캐릭터 그림은 칸보다 세로로 길어서(약 1.7배) 박스 높이를 칸 높이로 쓰면 위아래 칸이 틀어진다.
 * 그래서 칸 크기는 박스 너비(내 박스, 없으면 몹 박스 너비의 중간값)로 잡고,
 * 칸 위치는 박스 가운데가 아니라 발(아래쪽에서 반 칸 위)로 잡는다.
 */
class TileGrid(private val origin: Point, private val tileSize: Double) {
    fun tileOf(box: Rectangle): Tile {
        val c = box.foot(tileSize)
        return Tile(((c.x - origin.x) / tileSize).roundToInt(), ((c.y - origin.y) / tileSize).roundToInt())
    }

    companion object {
        fun of(detection: Detection): TileGrid? {
            val size = detection.me?.width
                ?: detection.monsters.map { it.width }.sorted().getOrNull(detection.monsters.size / 2)
            if (size == null || size <= 0) return null
            val origin = detection.me?.foot(size.toDouble()) ?: detection.myPosition
            return TileGrid(origin, size.toDouble())
        }

        /** 박스가 서 있는 칸의 가운데. 박스 아래쪽에서 반 칸 위 */
        private fun Rectangle.foot(tileSize: Double) = Point(x + width / 2, (y + height - tileSize / 2).roundToInt())
    }
}
