package ocr.character

import java.awt.image.BufferedImage

/**
 * 우하단 좌표 줄(예: "0039 0144")을 읽는다. OCR 서버를 쓰지 않는다.
 *
 * 좌표 숫자는 크기가 고정된 도트 글꼴(연노랑 + 검은 테두리)이라, 글자를 하나씩 잘라 7x9 칸으로 줄인 뒤
 * 미리 떠 둔 숫자 모양과 칸이 몇 개 같은지로 고른다. 창 크기가 달라도 7x9로 줄이므로 그대로 맞는다.
 */
object CoordinateReader {
    private const val GRID_WIDTH = 7
    private const val GRID_HEIGHT = 9
    /** 가장 닮은 숫자와 이만큼 이상 같아야 그 숫자로 본다 */
    private const val MIN_SIMILARITY = 0.85
    /** 두 번째로 닮은 숫자와 이만큼은 차이가 나야 한다 (3과 5가 78% 비슷하다) */
    private const val MIN_MARGIN = 0.05
    /** 좌표 한 쪽의 자릿수 (예: 0039) */
    private const val DIGITS_PER_NUMBER = 4

    data class Coordinate(val x: Int, val y: Int)

    /** 못 읽은 글자가 하나라도 있거나 숫자 묶음이 두 개가 아니면 null */
    fun read(image: BufferedImage): Coordinate? {
        val width = image.width
        val pixels = image.getRGB(0, 0, width, image.height, null, 0, width)
        val mask = BooleanArray(pixels.size) { isDigitColor(pixels[it]) }

        // 숫자 줄: 글자 픽셀이 있는 줄이 가장 길게 이어진 곳 (위아래 칸 구분선 조각은 따로 떨어져 있다)
        val rows = (0 until image.height).map { y -> (0 until width).any { x -> mask[y * width + x] } }
        val band = longestRun(rows) ?: return null

        // 글자: 글자 픽셀이 있는 열이 이어진 구간
        val columns = (0 until width).map { x -> band.any { y -> mask[y * width + x] } }
        val glyphs = runs(columns).map { xs ->
            val ys = band.filter { y -> xs.any { x -> mask[y * width + x] } }
            Glyph(xs, ys.first()..ys.last())
        }
        if (glyphs.isEmpty()) return null
        val tallest = glyphs.maxOf { it.rows.count() }
        val tall = glyphs.filter { it.rows.count() >= tallest * 0.6 }
        // 숫자는 모두 높이가 같다. 칸 구분선 조각이 위에 붙은 글자도 있으므로 위아래는 글자들의 중앙값으로 맞춘다
        val top = tall.map { it.rows.first }.sorted()[tall.size / 2]
        val bottom = tall.map { it.rows.last }.sorted()[tall.size / 2]
        // 영역 끝에 걸린 패널 테두리 조각은 숫자로 안 읽히므로 양 끝에서 떼어 낸다. 가운데 글자를 못 읽으면 실패
        val classified = tall.map { Glyph(it.columns, top..bottom) }.map { it to classify(it, mask, width) }
        val first = classified.indexOfFirst { it.second != null }
        val last = classified.indexOfLast { it.second != null }
        if (first < 0) return null
        val digits = classified.subList(first, last + 1)
        if (digits.size < 2 || digits.any { it.second == null }) return null

        // 두 숫자 사이 공백은 글자 사이 공백보다 훨씬 넓다
        val gaps = digits.zipWithNext { a, b -> b.first.columns.first - a.first.columns.last }
        val split = gaps.indices.maxBy { gaps[it] }
        if (gaps[split] < gaps.sorted()[gaps.size / 2] * 2) return null

        val text = digits.joinToString("") { it.second.toString() }
        // 좌표는 항상 0을 채운 네 자리씩이다. 끝 글자를 조각으로 잘못 떼어 냈으면 자릿수가 모자라므로 버린다
        val xText = text.substring(0, split + 1)
        val yText = text.substring(split + 1)
        if (xText.length != DIGITS_PER_NUMBER || yText.length != DIGITS_PER_NUMBER) return null
        val x = xText.toIntOrNull() ?: return null
        val y = yText.toIntOrNull() ?: return null
        return Coordinate(x, y)
    }

    private class Glyph(val columns: IntRange, val rows: IntRange)

    private fun classify(glyph: Glyph, mask: BooleanArray, width: Int): Char? {
        val cells = normalize(glyph, mask, width)
        val scores = TEMPLATES.mapValues { (_, template) ->
            var same = 0
            for (i in 0 until GRID_HEIGHT) for (j in 0 until GRID_WIDTH) {
                if ((template[i][j] == '#') == cells[i * GRID_WIDTH + j]) same++
            }
            same.toDouble() / (GRID_WIDTH * GRID_HEIGHT)
        }.entries.sortedByDescending { it.value }
        val best = scores[0]
        if (best.value < MIN_SIMILARITY || best.value - scores[1].value < MIN_MARGIN) return null
        return best.key
    }

    /** 글자를 7x9 칸으로 줄인다. 칸 안 픽셀의 절반 이상이 글자색이면 찬 칸 */
    private fun normalize(glyph: Glyph, mask: BooleanArray, width: Int): BooleanArray {
        val w = glyph.columns.count()
        val h = glyph.rows.count()
        return BooleanArray(GRID_WIDTH * GRID_HEIGHT) { index ->
            val i = index / GRID_WIDTH
            val j = index % GRID_WIDTH
            val y0 = i * h / GRID_HEIGHT
            val y1 = maxOf(y0 + 1, (i + 1) * h / GRID_HEIGHT)
            val x0 = j * w / GRID_WIDTH
            val x1 = maxOf(x0 + 1, (j + 1) * w / GRID_WIDTH)
            var on = 0
            for (y in y0 until y1) for (x in x0 until x1) {
                if (mask[(glyph.rows.first + y) * width + glyph.columns.first + x]) on++
            }
            on * 2 >= (y1 - y0) * (x1 - x0)
        }
    }

    /** 숫자 안쪽 연노랑 (247,227,156) 등. 갈색 바탕과 검은 테두리는 빠진다 */
    private fun isDigitColor(rgb: Int): Boolean {
        val r = rgb shr 16 and 0xFF
        val g = rgb shr 8 and 0xFF
        val b = rgb and 0xFF
        return r >= 170 && g >= 150 && r - b >= 40
    }

    private fun runs(on: List<Boolean>): List<IntRange> {
        val out = mutableListOf<IntRange>()
        var start = -1
        for (i in on.indices) {
            if (on[i] && start < 0) start = i
            if (!on[i] && start >= 0) { out += start until i; start = -1 }
        }
        if (start >= 0) out += start until on.size
        return out
    }

    private fun longestRun(on: List<Boolean>): IntRange? = runs(on).maxByOrNull { it.count() }

    /**
     * 실제 게임 화면 좌표 줄과 돈 줄에서 뜬 7x9 숫자 모양.
     */
    private val TEMPLATES: Map<Char, List<String>> = mapOf(
        '0' to listOf(
            "..###..",
            ".#####.",
            "###..##",
            "###..##",
            "###..##",
            "###..##",
            "###..##",
            ".#####.",
            "..###..",
        ),
        '1' to listOf(
            "..###..",
            ".####..",
            "#####..",
            "..###..",
            "..###..",
            "..###..",
            "..###..",
            ".######",
            "######.",
        ),
        '2' to listOf(
            "..###..",
            ".#####.",
            "##..##.",
            "....##.",
            "....##.",
            "...##..",
            "..##...",
            ".######",
            "######.",
        ),
        '3' to listOf(
            "..#####",
            ".######",
            "....##.",
            "...##..",
            "..###..",
            "...###.",
            "....##.",
            ".#####.",
            "#####..",
        ),
        '4' to listOf(
            ".....##",
            "....###",
            "..#####",
            ".###.##",
            "###..##",
            "#######",
            "#######",
            ".....##",
            ".....##",
        ),
        '5' to listOf(
            "..#####",
            "..####.",
            ".##....",
            ".####..",
            ".#####.",
            "....##.",
            "...###.",
            ".####..",
            "####...",
        ),
        '6' to listOf(
            "....###",
            "..####.",
            ".###...",
            ".#####.",
            "###..##",
            "###..##",
            "###.###",
            "######.",
            ".####..",
        ),
        '7' to listOf(
            ".######",
            "#######",
            ".....##",
            "....##.",
            "..###..",
            ".###...",
            "####...",
            "###....",
            "###....",
        ),
        '8' to listOf(
            "..####.",
            ".##..##",
            ".##.###",
            "..####.",
            ".####..",
            "##..##.",
            "##..##.",
            "##.###.",
            ".####..",
        ),
        '9' to listOf(
            "..####.",
            ".###.##",
            "###..##",
            "###..##",
            "#######",
            ".#####.",
            "....##.",
            ".####..",
            "####...",
        ),
    )
}
