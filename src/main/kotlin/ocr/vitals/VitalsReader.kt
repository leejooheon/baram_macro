package ocr.vitals

import java.awt.Rectangle
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

/**
 * 우하단 상태 패널의 체력(빨강)/마력(파랑) 막대가 얼마나 찼는지 픽셀 색으로 잰다. OCR 서버를 쓰지 않는다.
 *
 * 화면에는 현재값(예: 585507)만 나오고 최대값이 없어서 숫자로는 %를 알 수 없다. 대신 막대 칸을 열 단위로
 * "찬 칸(막대 색)"과 "빈 칸(거의 검정 (8,4,8))"으로 나눠 비율을 잰다.
 * - 체력 막대는 오른쪽에 붙어서 왼쪽부터 빈다. 어느 쪽에서 비든 칸 수로 세므로 방향과 상관없다.
 * - 오른쪽에 겹친 숫자는 테두리가 순수 검정(0,0,0)이라 빈 칸 색과 구분된다. 숫자만 있는 열은 양옆 칸을 따른다.
 */
object VitalsReader {
    /** [hpBox]/[mpBox]는 영역 이미지 픽셀 기준 막대 칸 전체 */
    data class BarReading(
        val hpPercent: Int,
        val mpPercent: Int?,
        val hpBox: Rectangle,
        val mpBox: Rectangle?,
    )

    private class Band(val rows: IntRange, val left: Int, val right: Int, val filled: BooleanArray, val empty: BooleanArray)

    /** 막대를 찾지 못하면 null */
    fun read(image: BufferedImage): BarReading? {
        val width = image.width
        val height = image.height
        val pixels = image.getRGB(0, 0, width, height, null, 0, width)
        val red = BooleanArray(pixels.size) { isHpColor(pixels[it]) }
        val blue = BooleanArray(pixels.size) { isMpColor(pixels[it]) }
        val empty = BooleanArray(pixels.size) { isEmptyColor(pixels[it]) }
        val bar = BooleanArray(pixels.size) { red[it] || blue[it] || empty[it] }

        // 막대 칸(찬 곳 + 빈 곳)이 많이 보이는 줄들을 묶어 막대 후보로 만든다
        val counts = IntArray(height) { y -> (0 until width).count { x -> bar[y * width + x] } }
        val maxCount = counts.max()
        if (maxCount < 10) return null
        val rows = (0 until height).filter { counts[it] * 2 >= maxCount }
        val groups = mutableListOf<MutableList<Int>>()
        for (y in rows) {
            val last = groups.lastOrNull()
            // 마력 막대 가운데의 아주 밝은 줄은 막대 색에서 빠지므로 몇 줄 끊겨도 같은 막대로 본다
            if (last != null && y - last.last() <= 4) last += y else groups += mutableListOf(y)
        }
        val gap = maxOf(6, width / 10)
        val bands = groups
            .filter { it.last() - it.first() >= 2 }
            .map { group ->
                val range = group.first()..group.last()
                val any = BooleanArray(width) { x -> range.any { y -> bar[y * width + x] } }
                val (left, right) = longestRun(any, gap)
                Band(range, left, right, BooleanArray(0), BooleanArray(0))
            }
        if (bands.isEmpty()) return null

        fun columns(band: Band, color: BooleanArray): Band {
            val need = maxOf(1, (band.rows.count() * 0.3).toInt())
            val filled = BooleanArray(band.right - band.left + 1) { i ->
                band.rows.any { y -> color[y * width + band.left + i] }
            }
            val emptyColumns = BooleanArray(filled.size) { i ->
                !filled[i] && band.rows.count { y -> empty[y * width + band.left + i] } >= need
            }
            return Band(band.rows, band.left, band.right, filled, emptyColumns)
        }

        // 체력: 빨강이 가장 많은 막대. 체력이 0이면 빈 칸이 가장 많은 막대
        val withRed = bands.map { columns(it, red) }
        val hpIndex = withRed.indices.maxBy { i -> withRed[i].filled.count { it } * 10_000 + withRed[i].empty.count { it } }
        val hp = withRed[hpIndex]
        // 마력: 체력 바로 아래 막대 (위쪽 능력치 줄의 하늘색 숫자는 체력보다 위라서 걸리지 않는다)
        val mp = bands.drop(hpIndex + 1)
            .firstOrNull { it.rows.first - hp.rows.last <= 3 * hp.rows.count() }
            ?.let { columns(it, blue) }

        return BarReading(
            hpPercent = percent(hp),
            mpPercent = mp?.let(::percent),
            hpBox = hp.box(),
            mpBox = mp?.box(),
        )
    }

    private fun Band.box() = Rectangle(left, rows.first, right - left + 1, rows.count())

    /** 찬 칸 비율. 숫자에 완전히 가려진 열은 양옆 칸 값을 따르고, 양옆이 다르면 반반으로 친다 */
    private fun percent(band: Band): Int {
        val n = band.filled.size
        val state = IntArray(n) { i -> if (band.filled[i]) 1 else if (band.empty[i]) 0 else -1 }
        if (state.all { it < 0 }) return 0
        var filled = 0.0
        var i = 0
        while (i < n) {
            if (state[i] >= 0) {
                filled += state[i]
                i++
                continue
            }
            var j = i
            while (j < n && state[j] < 0) j++
            val neighbors = listOfNotNull(state.getOrNull(i - 1), state.getOrNull(j))
            filled += (j - i) * neighbors.average()
            i = j
        }
        return (filled * 100 / n).roundToInt().coerceIn(0, 100)
    }

    /** [gap]보다 짧게 끊긴 곳은 이어 붙여서 가장 긴 구간 (start, end) */
    private fun longestRun(columns: BooleanArray, gap: Int): Pair<Int, Int> {
        var best = 0 to -1
        var start = -1
        var last = -1
        for (x in columns.indices) {
            if (!columns[x]) continue
            if (start < 0 || x - last > gap) start = x
            last = x
            if (last - start > best.second - best.first) best = start to last
        }
        return best
    }

    /** 막대 가운데 줄 (255,77,0), (231,65,0), (181,44,0). 옆 테두리의 주황/갈색은 파란 성분이 있어서 빠진다 */
    private fun isHpColor(rgb: Int): Boolean {
        val r = rgb shr 16 and 0xFF
        val g = rgb shr 8 and 0xFF
        val b = rgb and 0xFF
        return r >= 170 && b <= 15 && g * 2 <= r
    }

    /** (123,146,189), (82,101,156) 등 */
    private fun isMpColor(rgb: Int): Boolean {
        val r = rgb shr 16 and 0xFF
        val b = rgb and 0xFF
        return b >= 150 && b - r >= 50
    }

    /** 빈 막대 칸 (8,4,8). 숫자 테두리의 순수 검정 (0,0,0)과 구분한다 */
    private fun isEmptyColor(rgb: Int): Boolean {
        val r = rgb shr 16 and 0xFF
        val g = rgb shr 8 and 0xFF
        val b = rgb and 0xFF
        return r in 3..20 && b in 3..20 && g <= r
    }
}
