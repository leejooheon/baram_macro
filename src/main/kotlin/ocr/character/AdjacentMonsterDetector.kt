package ocr.character

import ocr.model.Direction
import java.awt.Rectangle
import java.awt.image.BufferedImage

/**
 * 내 캐릭터 바로 옆 네 칸(상하좌우)에 등록한 몬스터가 서 있는지 본다. OCR 서버를 쓰지 않는다.
 *
 * 등록한 몬스터 그림에서 "그 몬스터 색"(그림 테두리의 바닥색, 지금 맵에 흔한 색, 내 캐릭터 색을 뺀 것)인 픽셀만
 * 골라 그 자리와 색을 기억해 둔다. 옆 칸마다 몬스터 발밑이 그 칸에 오는 위치들을 훑으면서, 기억한 픽셀 중
 * 같은 자리에 같은 색이 있는 비율이 가장 높은 곳을 찾는다. 색 분포가 아니라 모양까지 맞춰 보므로
 * 바닥 무늬, 바위, 핏자국처럼 색만 비슷한 것은 걸러지고, 스킬 이펙트(노랑, 흰색)는 몬스터 색이 아니라서 빠진다.
 * 몬스터가 보는 방향마다 모양이 다르므로 앞/뒤/옆 모습을 각각 등록하는 것이 좋다.
 */
object AdjacentMonsterDetector {
    private const val BINS = CharacterLocator.BINS
    /** 몬스터 그림에서 이 비율 이상 나오는 색만 몬스터 색 후보로 본다 */
    private const val MIN_SAMPLE_SHARE = 0.005
    /** 몬스터 그림 테두리 쪽에서 이 비율 이상 보이는 색은 바닥으로 보고 뺀다 */
    private const val MAX_BACKGROUND_SHARE = 0.02
    /** 맵 전체에서 이 비율 이상 보이는 색은 흔해서 뺀다 (몬스터가 많이 모여도 그 색이 빠지지 않게 넉넉히 잡는다) */
    private const val MAX_FIELD_SHARE = 0.01
    /** 기억한 픽셀 중 이 비율 이상이 같은 자리 같은 색이면 몬스터가 있다고 본다 */
    const val MIN_SCORE = 0.3

    data class Monster(
        /** 맵 영역 이미지 픽셀 기준 몬스터 칸 */
        val box: Rectangle,
        /** 기억한 몬스터 픽셀 중 같은 자리 같은 색 비율 */
        val score: Double,
        val direction: Direction,
    )

    data class Reading(
        /** 방향별 캐릭터 옆 칸 (맵 영역 이미지 픽셀 기준, 썸네일에 그린다) */
        val cells: Map<Direction, Rectangle>,
        /** 방향별 가장 잘 맞은 점수 (몬스터가 없어도 들어 있다) */
        val scores: Map<Direction, Double>,
        /** 옆 칸에서 찾은 몬스터 */
        val monsters: List<Monster>,
    ) {
        val occupied: Set<Direction> get() = monsters.map { it.direction }.toSet()
    }

    /**
     * [character]는 내 캐릭터 칸, [tileSize]는 맵 한 칸의 픽셀 크기, [samples]는 등록한 몬스터 그림.
     * 캐릭터와 몬스터 그림은 칸보다 위로 솟아 있으므로 그림 아래쪽 한 칸을 선 칸으로 본다.
     */
    fun detect(
        field: BufferedImage,
        character: CharacterLocator.Reading,
        tileSize: Int,
        samples: List<BufferedImage>,
    ): Reading {
        val width = field.width
        val height = field.height
        val box = character.box
        val standX = box.x + box.width / 2 - tileSize / 2
        val standY = box.y + box.height - tileSize
        val bounds = Rectangle(0, 0, width, height)
        val cells = Direction.entries.associateWith { direction ->
            Rectangle(standX + direction.dx * tileSize, standY + direction.dy * tileSize, tileSize, tileSize)
                .intersection(bounds)
        }
        val scores = Direction.entries.associateWith { 0.0 }.toMutableMap()
        val best = mutableMapOf<Direction, Monster>()
        if (samples.isEmpty()) return Reading(cells, scores, emptyList())

        val bins = CharacterLocator.bins(field)
        val fieldCounts = IntArray(BINS).also { c -> bins.forEach { c[it]++ } }
        val step = maxOf(1, tileSize / 24)
        for (sample in samples) {
            val shape = sampleShape(sample, fieldCounts, bins.size, character.colorBins) ?: continue
            for (direction in Direction.entries) {
                // 몬스터 발밑(몬스터 색 픽셀의 아래 가운데)이 옆 칸 근처에 오는 위치만 훑는다.
                // 붙어 있어도 칸에서 20~25px씩 벗어나 보이므로 좌우와 아래로 칸의 1/3씩 넓힌다
                val tileX = standX + direction.dx * tileSize
                val tileY = standY + direction.dy * tileSize
                val slack = tileSize / 3
                for (footY in tileY + tileSize / 4..tileY + tileSize + slack step step) {
                    val oy = footY - shape.footY
                    if (oy < 0 || oy + shape.height > height) continue
                    for (footX in tileX - slack..tileX + tileSize + slack step step) {
                        val ox = footX - shape.footX
                        if (ox < 0 || ox + shape.width > width) continue
                        var same = 0
                        for (i in shape.xs.indices) {
                            if (bins[(oy + shape.ys[i]) * width + ox + shape.xs[i]] == shape.bins[i]) same++
                        }
                        val score = same.toDouble() / shape.xs.size
                        if (score > scores.getValue(direction)) {
                            scores[direction] = score
                            if (score >= MIN_SCORE) {
                                best[direction] = Monster(Rectangle(ox, oy, shape.width, shape.height), score, direction)
                            }
                        }
                    }
                }
            }
        }
        return Reading(cells, scores, best.values.toList())
    }

    /** 몬스터 색인 픽셀의 자리(그림 왼쪽 위 기준)와 색 칸 */
    private class SampleShape(
        val xs: IntArray,
        val ys: IntArray,
        val bins: IntArray,
        val width: Int,
        val height: Int,
        /** 몬스터 색 픽셀의 아래 가운데. 드래그할 때 들어간 바닥 여백과 상관없이 발밑으로 쓴다 */
        val footX: Int,
        val footY: Int,
    )

    private fun sampleShape(sample: BufferedImage, fieldCounts: IntArray, fieldTotal: Int, characterBins: Set<Int>): SampleShape? {
        val w = sample.width
        val h = sample.height
        if (w < 8 || h < 8) return null
        val bins = CharacterLocator.bins(sample)
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
        val xs = ArrayList<Int>()
        val ys = ArrayList<Int>()
        val keyBins = ArrayList<Int>()
        for (y in 0 until h) for (x in 0 until w) {
            val b = bins[y * w + x]
            if (isKey[b]) { xs += x; ys += y; keyBins += b }
        }
        // 몬스터 색이 너무 적으면 (지금 맵에 흔한 색뿐이면) 이 그림으로는 찾지 않는다
        if (xs.size < 20) return null
        val sortedX = xs.sorted()
        val sortedY = ys.sorted()
        return SampleShape(
            xs = xs.toIntArray(),
            ys = ys.toIntArray(),
            bins = keyBins.toIntArray(),
            width = w,
            height = h,
            footX = sortedX[sortedX.size / 2],
            footY = sortedY[sortedY.size * 98 / 100],
        )
    }
}
