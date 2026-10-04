package ocr.character

import ocr.model.Direction
import java.awt.Rectangle
import java.awt.image.BufferedImage

/**
 * 내 캐릭터 바로 옆 네 칸(상하좌우)에 등록한 몬스터가 붙어 있는지 본다. OCR 서버를 쓰지 않는다.
 *
 * 내 캐릭터를 찾을 때와 같은 방식이다. 등록한 몬스터 그림에서 "그 몬스터에만 있는 색"(그림 테두리의 바닥색,
 * 지금 맵에 흔한 색, 내 캐릭터 색을 뺀 것)을 골라, 옆 칸마다 그 색이 몬스터 그림 대비 얼마나 있는지 센다.
 * 바닥과 같은 색인 몸통은 빠지지만 눈 같은 부분이 남고, 스킬 이펙트(노랑, 흰색)는 몬스터 색이 아니라서 걸러진다.
 */
object AdjacentMonsterDetector {
    private const val BINS = CharacterLocator.BINS
    /** 몬스터 그림에서 이 비율 이상 나오는 색만 몬스터 색 후보로 본다 */
    private const val MIN_SAMPLE_SHARE = 0.005
    /** 몬스터 그림 테두리 쪽에서 이 비율 이상 보이는 색은 바닥으로 보고 뺀다 */
    private const val MAX_BACKGROUND_SHARE = 0.02
    /** 맵 전체에서 이 비율 이상 보이는 색은 흔해서 뺀다 (몬스터가 많이 모여도 그 색이 빠지지 않게 넉넉히 잡는다) */
    private const val MAX_FIELD_SHARE = 0.01
    /** 옆 칸의 몬스터 색 픽셀이 몬스터 그림 대비 이 비율 이상이면 붙어 있다고 본다 */
    const val MIN_OCCUPIED = 0.3

    data class Reading(
        /** 방향별로 몬스터를 찾아본 곳 (맵 영역 이미지 픽셀 기준) */
        val cells: Map<Direction, Rectangle>,
        /** 방향별 몬스터 색 비율 (등록한 몬스터 중 가장 높은 값, 1이면 몬스터 하나만큼) */
        val ratios: Map<Direction, Double>,
    ) {
        val occupied: Set<Direction> get() = ratios.filterValues { it >= MIN_OCCUPIED }.keys
    }

    /**
     * [character]는 내 캐릭터 칸, [tileSize]는 맵 한 칸의 픽셀 크기, [monsters]는 등록한 몬스터 그림.
     * 캐릭터 그림은 칸보다 위로 솟아 있으므로 그림 아래쪽 한 칸을 캐릭터가 선 칸으로 본다.
     * 몬스터 그림은 가로 약 1.5칸, 세로 약 2칸이고 발끝이 칸 아래로 조금 내려오므로 옆 칸보다 넓게 본다.
     */
    fun detect(
        field: BufferedImage,
        character: CharacterLocator.Reading,
        tileSize: Int,
        monsters: List<BufferedImage>,
    ): Reading {
        val width = field.width
        val height = field.height
        val box = character.box
        val standX = box.x + box.width / 2 - tileSize / 2
        val standY = box.y + box.height - tileSize
        val bounds = Rectangle(0, 0, width, height)
        val cells = Direction.entries.associateWith { direction ->
            val tileX = standX + direction.dx * tileSize
            val tileY = standY + direction.dy * tileSize
            Rectangle(tileX - tileSize / 4, tileY - tileSize, tileSize * 3 / 2, tileSize * 2 + tileSize / 3)
                .intersection(bounds)
        }
        if (monsters.isEmpty()) return Reading(cells, Direction.entries.associateWith { 0.0 })

        val bins = CharacterLocator.bins(field)
        val fieldCounts = IntArray(BINS).also { c -> bins.forEach { c[it]++ } }
        val ratios = Direction.entries.associateWith { 0.0 }.toMutableMap()
        for (monster in monsters) {
            val colors = monsterColors(monster, fieldCounts, bins.size, character.colorBins) ?: continue
            for ((direction, cell) in cells) {
                if (cell.isEmpty) continue
                var hit = 0
                for (y in cell.y until cell.y + cell.height) for (x in cell.x until cell.x + cell.width) {
                    // 위 칸은 내 캐릭터 머리가 덮고 있으므로 캐릭터 그림 칸은 빼고 센다
                    if (!box.contains(x, y) && colors.isKey[bins[y * width + x]]) hit++
                }
                val ratio = hit.toDouble() / colors.expected
                if (ratio > ratios.getValue(direction)) ratios[direction] = ratio
            }
        }
        return Reading(cells, ratios)
    }

    private class MonsterColors(val isKey: BooleanArray, val expected: Int)

    private fun monsterColors(monster: BufferedImage, fieldCounts: IntArray, fieldTotal: Int, characterBins: Set<Int>): MonsterColors? {
        val w = monster.width
        val h = monster.height
        if (w < 8 || h < 8) return null
        val bins = CharacterLocator.bins(monster)
        val counts = IntArray(BINS).also { c -> bins.forEach { c[it]++ } }
        val border = maxOf(2, minOf(w, h) / 12)
        val borderCounts = IntArray(BINS)
        var borderTotal = 0
        for (y in 0 until h) for (x in 0 until w) {
            if (x < border || x >= w - border || y < border || y >= h - border) {
                borderCounts[bins[y * w + x]]++
                borderTotal++
            }
        }
        val isKey = BooleanArray(BINS) { b ->
            counts[b] >= bins.size * MIN_SAMPLE_SHARE &&
                borderCounts[b] < borderTotal * MAX_BACKGROUND_SHARE &&
                fieldCounts[b] < fieldTotal * MAX_FIELD_SHARE &&
                b !in characterBins
        }
        val expected = bins.count { isKey[it] }
        // 몬스터 색이 너무 적으면 (지금 맵에 흔한 색뿐이면) 이 몬스터로는 판단하지 않는다
        return if (expected < 20) null else MonsterColors(isKey, expected)
    }
}
