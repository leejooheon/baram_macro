package detector

import java.awt.Point
import java.awt.Rectangle
import kotlin.math.roundToInt

/** 게임 칸 좌표. 내 캐릭터가 (0, 0) */
data class Tile(val x: Int, val y: Int) {
    val neighbors: List<Tile> get() = listOf(Tile(x, y - 1), Tile(x, y + 1), Tile(x - 1, y), Tile(x + 1, y))
    fun isNeighborOf(other: Tile) = other in neighbors

    /** 내가 [step]만큼 움직인 뒤 이 칸(나 기준 좌표)이 어디로 보이는지 */
    fun afterMyMove(step: Tile) = Tile(x - step.x, y - step.y)
}

/**
 * 탐지 박스를 칸 좌표로 바꾼다. 바람의 칸은 정사각형이고 크기는 창 너비에 비례한다
 * (창 너비 2560px에서 72.16px: 플레이 영역 1804px / 25칸, 세로 1084px / 15칸으로 잰 값).
 * 박스 크기는 탐지마다 51~117px로 흔들리고 캐릭터 그림은 칸보다 세로로 길어서 칸 크기로 쓰지 않는다.
 * 칸 위치는 박스 가운데가 아니라 발(아래쪽에서 반 칸 위)로 잡는다.
 */
class TileGrid(private val origin: Point, private val tileSize: Double) {
    fun tileOf(box: Rectangle): Tile {
        val c = box.foot(tileSize)
        return Tile(((c.x - origin.x) / tileSize).roundToInt(), ((c.y - origin.y) / tileSize).roundToInt())
    }

    companion object {
        /** 창 너비 대비 한 칸 크기 */
        const val TILE_RATIO = 72.16 / 2560

        fun of(detection: Detection): TileGrid? {
            val size = detection.windowWidth * TILE_RATIO
            if (size <= 0) return null
            val origin = detection.me?.foot(size) ?: detection.myPosition
            return TileGrid(origin, size)
        }

        /** 박스가 서 있는 칸의 가운데. 박스 아래쪽에서 반 칸 위 */
        private fun Rectangle.foot(tileSize: Double) = Point(x + width / 2, (y + height - tileSize / 2).roundToInt())
    }
}
