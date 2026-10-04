package ocr.character

import ocr.model.Direction
import java.awt.Rectangle
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 맵 화면에서 등록한 몬스터를 모두 찾고, 그중 내 캐릭터 바로 옆 네 칸(상하좌우)에 선 몬스터를 고른다. OCR 서버를 쓰지 않는다.
 *
 * 내 캐릭터를 찾을 때와 같은 방식이다. 등록한 몬스터 그림에서 "그 몬스터에만 있는 색"(그림 테두리의 바닥색,
 * 지금 맵에 흔한 색, 내 캐릭터 색을 뺀 것)을 골라, 맵에서 그 색이 몬스터 그림만큼 모인 곳을 몬스터로 본다.
 * 그다음 몬스터 발밑 칸이 캐릭터 발밑 칸에서 몇 칸 떨어졌는지로 방향을 정하므로, 대각선 몬스터나
 * 칸에 반쯤 걸친 몬스터도 헷갈리지 않는다. 스킬 이펙트(노랑, 흰색)는 몬스터 색이 아니라서 걸러진다.
 */
object AdjacentMonsterDetector {
    private const val BINS = CharacterLocator.BINS
    /** 몬스터 그림에서 이 비율 이상 나오는 색만 몬스터 색 후보로 본다 */
    private const val MIN_SAMPLE_SHARE = 0.005
    /** 몬스터 그림 테두리 쪽에서 이 비율 이상 보이는 색은 바닥으로 보고 뺀다 */
    private const val MAX_BACKGROUND_SHARE = 0.02
    /** 맵 전체에서 이 비율 이상 보이는 색은 흔해서 뺀다 (몬스터가 많이 모여도 그 색이 빠지지 않게 넉넉히 잡는다) */
    private const val MAX_FIELD_SHARE = 0.01
    /** 맵의 한 곳에 몬스터 색이 몬스터 그림 대비 이 비율 이상 모였으면 몬스터로 본다 */
    const val MIN_SCORE = 0.45

    data class Monster(
        /** 맵 영역 이미지 픽셀 기준 몬스터 칸 */
        val box: Rectangle,
        /** 몬스터 그림 대비 몬스터 색 비율 */
        val score: Double,
        /** 캐릭터 발밑 칸에서 몇 칸 떨어졌는지 (오른쪽/아래가 +) */
        val dx: Int,
        val dy: Int,
    ) {
        /** 바로 옆 네 칸 중 하나면 그 방향, 아니면 null */
        val direction: Direction? get() = Direction.entries.firstOrNull { it.dx == dx && it.dy == dy }
    }

    data class Reading(
        /** 방향별 캐릭터 옆 칸 (맵 영역 이미지 픽셀 기준, 썸네일에 그린다) */
        val cells: Map<Direction, Rectangle>,
        /** 맵에서 찾은 몬스터 전부 */
        val monsters: List<Monster>,
    ) {
        val occupied: Set<Direction> get() = monsters.mapNotNull { it.direction }.toSet()
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
        if (samples.isEmpty()) return Reading(cells, emptyList())

        val bins = CharacterLocator.bins(field)
        val fieldCounts = IntArray(BINS).also { c -> bins.forEach { c[it]++ } }
        // 걷는 자세나 스킬 이펙트로 캐릭터 그림 바깥에 걸친 픽셀이 세어지지 않게 캐릭터 칸은 조금 넓혀서 뺀다
        val margin = tileSize / 4
        val exclude = Rectangle(box.x - margin, box.y - margin, box.width + margin * 2, box.height + margin * 2)

        val found = mutableListOf<Monster>()
        for (sample in samples) {
            val colors = sampleColors(sample, fieldCounts, bins.size, character.colorBins) ?: continue
            val w = colors.width.coerceAtMost(width)
            val h = colors.height.coerceAtMost(height)
            // 몬스터 색 픽셀 누적합으로 w x h 칸마다 개수를 센다
            val stride = width + 1
            val sums = IntArray(stride * (height + 1))
            for (y in 0 until height) {
                var row = 0
                for (x in 0 until width) {
                    if (colors.isKey[bins[y * width + x]] && !exclude.contains(x, y)) row++
                    sums[(y + 1) * stride + x + 1] = sums[y * stride + x + 1] + row
                }
            }
            fun count(x: Int, y: Int) =
                sums[(y + h) * stride + x + w] - sums[y * stride + x + w] - sums[(y + h) * stride + x] + sums[y * stride + x]

            // 점수가 높은 곳부터 고르고, 고른 곳과 많이 겹치는 곳은 같은 몬스터로 보고 건너뛴다
            val need = (colors.expected * MIN_SCORE).toInt().coerceAtLeast(1)
            val step = maxOf(1, tileSize / 12)
            val candidates = mutableListOf<Triple<Int, Int, Int>>()
            for (y in 0..height - h step step) for (x in 0..width - w step step) {
                val c = count(x, y)
                if (c >= need) candidates += Triple(c, x, y)
            }
            candidates.sortByDescending { it.first }
            for ((c, x, y) in candidates) {
                val rect = Rectangle(x, y, w, h)
                if (found.any { it.box.overlapRatio(rect) > 0.3 }) continue
                val footX = x + w / 2
                val footY = y + h - tileSize / 2
                found += Monster(
                    box = rect,
                    score = c.toDouble() / colors.expected,
                    dx = ((footX - (standX + tileSize / 2)).toDouble() / tileSize).roundToInt(),
                    dy = ((footY - (standY + tileSize / 2)).toDouble() / tileSize).roundToInt(),
                )
            }
        }
        return Reading(cells, found)
    }

    /** 작은 쪽 넓이 대비 겹친 넓이 */
    private fun Rectangle.overlapRatio(other: Rectangle): Double {
        val i = intersection(other)
        if (i.isEmpty) return 0.0
        return (i.width.toDouble() * i.height) / minOf(width * height, other.width * other.height)
    }

    private class SampleColors(val isKey: BooleanArray, val expected: Int, val width: Int, val height: Int)

    /** 등록한 몬스터 그림에서 몬스터 색을 고르고, 그 색이 있는 곳의 크기(5%~95%)와 개수를 잰다 */
    private fun sampleColors(sample: BufferedImage, fieldCounts: IntArray, fieldTotal: Int, characterBins: Set<Int>): SampleColors? {
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
        for (y in 0 until h) for (x in 0 until w) {
            if (isKey[bins[y * w + x]]) { xs += x; ys += y }
        }
        // 몬스터 색이 너무 적으면 (지금 맵에 흔한 색뿐이면) 이 그림으로는 찾지 않는다
        if (xs.size < 20) return null
        xs.sort(); ys.sort()
        val x0 = xs[xs.size * 5 / 100]
        val x1 = xs[xs.size * 95 / 100]
        val y0 = ys[ys.size * 5 / 100]
        val y1 = ys[ys.size * 95 / 100]
        var expected = 0
        for (y in y0..y1) for (x in x0..x1) if (isKey[bins[y * w + x]]) expected++
        if (expected < 20) return null
        // 몬스터 발끝은 색이 있는 곳보다 조금 아래이므로 높이는 그림 아래 끝까지 잡는다
        return SampleColors(isKey, expected, x1 - x0 + 1, h - y0)
    }
}
