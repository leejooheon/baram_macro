package ocr.character

import ocr.model.Direction
import java.awt.Rectangle
import java.awt.image.BufferedImage

/**
 * 내 캐릭터 바로 옆 네 칸(상하좌우)에 무언가 서 있는지 본다. OCR 서버를 쓰지 않는다.
 *
 * 맵 바닥(풀, 땅, 길)은 화면에 흔한 색이고, 몬스터는 바닥에 드문 색으로 그려진다. 그래서 옆 칸마다
 * "지금 맵에서 드문 색"인 픽셀이 얼마나 되는지 세고, 많으면 몬스터가 붙어 있다고 본다. 내 캐릭터 색은 뺀다.
 */
object AdjacentMonsterDetector {
    /** 맵 전체에서 이 비율보다 적게 보이는 색을 "드문 색"으로 본다 */
    private const val RARE_SHARE = 0.003
    /** 옆 칸에서 드문 색 픽셀이 이 비율 이상이면 무언가 서 있다고 본다 */
    const val MIN_OCCUPIED = 0.15

    data class Reading(
        /** 방향별 옆 칸 (맵 영역 이미지 픽셀 기준) */
        val cells: Map<Direction, Rectangle>,
        /** 방향별 드문 색 픽셀 비율 */
        val ratios: Map<Direction, Double>,
    ) {
        val occupied: Set<Direction> get() = ratios.filterValues { it >= MIN_OCCUPIED }.keys
    }

    /**
     * [character]는 내 캐릭터 칸, [tileSize]는 맵 한 칸의 픽셀 크기.
     * 캐릭터 그림은 칸보다 위로 솟아 있으므로 발밑(그림 아래쪽) 한 칸을 캐릭터가 선 칸으로 본다.
     */
    fun detect(field: BufferedImage, character: CharacterLocator.Reading, tileSize: Int): Reading {
        val width = field.width
        val height = field.height
        val bins = CharacterLocator.bins(field)
        val counts = IntArray(CharacterLocator.BINS).also { c -> bins.forEach { c[it]++ } }
        val rare = BooleanArray(CharacterLocator.BINS) { b ->
            counts[b] < bins.size * RARE_SHARE && b !in character.colorBins
        }

        val box = character.box
        val standX = box.x + box.width / 2 - tileSize / 2
        val standY = box.y + box.height - tileSize
        val bounds = Rectangle(0, 0, width, height)
        val cells = Direction.entries.associateWith { direction ->
            Rectangle(standX + direction.dx * tileSize, standY + direction.dy * tileSize, tileSize, tileSize)
                .intersection(bounds)
        }
        val ratios = cells.mapValues { (_, cell) ->
            if (cell.isEmpty) return@mapValues 0.0
            // 위 칸은 내 캐릭터 머리가 덮고 있으므로 캐릭터 그림 칸은 빼고 센다
            var hit = 0
            var total = 0
            for (y in cell.y until cell.y + cell.height) for (x in cell.x until cell.x + cell.width) {
                if (box.contains(x, y)) continue
                total++
                if (rare[bins[y * width + x]]) hit++
            }
            if (total == 0) 0.0 else hit.toDouble() / total
        }
        return Reading(cells, ratios)
    }
}
