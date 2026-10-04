package ocr.vitals

import java.awt.image.BufferedImage

/**
 * 우하단 상태 패널의 체력(빨강)/마력(파랑) 막대가 얼마나 찼는지 픽셀 색으로 잰다. OCR 서버를 쓰지 않는다.
 *
 * 화면에는 현재값(예: 585507)만 나오고 최대값이 없어서 숫자로는 %를 알 수 없다. 대신 막대가 찬 폭을 잰다.
 * 숫자는 막대 오른쪽에 겹쳐 그려지므로, 글자 사이로 보이는 막대 색을 이어서 오른쪽 끝을 찾는다.
 */
object VitalsReader {
    /** 영역 이미지 픽셀 기준. 막대 시작(left)부터 찬 폭. 막대가 비었으면 폭 0 */
    data class BarReading(
        val left: Int,
        val hpWidth: Int,
        val mpWidth: Int,
        val hpRows: IntRange,
        val mpRows: IntRange?,
    )

    /** 막대를 찾지 못하면 null (체력 막대가 기준이라 체력이 아예 안 보이면 못 읽는다) */
    fun read(image: BufferedImage): BarReading? {
        val width = image.width
        val height = image.height
        val pixels = image.getRGB(0, 0, width, height, null, 0, width)
        val red = BooleanArray(pixels.size) { isHpColor(pixels[it]) }
        val blue = BooleanArray(pixels.size) { isMpColor(pixels[it]) }
        val gap = maxOf(6, width / 10)

        // 체력 막대: 빨간색이 가장 길게 이어진 줄들
        val runs = IntArray(height) { y -> longestRun(red, y * width, width) }
        val maxRun = runs.max()
        if (maxRun < 3) return null
        val bandRows = (0 until height).filter { runs[it] >= maxRun / 2 }
        val hpRows = bandRows.first()..bandRows.last()
        val hpColumns = filledColumns(red, width, hpRows)
        val left = hpColumns.indexOfFirst { it }.takeIf { it >= 0 } ?: return null
        val hpRight = filledRight(hpColumns, left, gap)

        // 마력 막대: 체력 막대 바로 아래에서, 같은 위치에서 시작하는 파란 줄들
        val searchEnd = minOf(height, hpRows.last + 1 + 3 * hpRows.count())
        val mpRowList = (hpRows.last + 1 until searchEnd).filter { y ->
            (left until minOf(width, left + 10)).count { x -> blue[y * width + x] } >= 3
        }
        val mpRows = mpRowList.takeIf { it.isNotEmpty() }?.let { it.first()..it.last() }
        val mpRight = mpRows?.let { filledRight(filledColumns(blue, width, it), left, gap) } ?: (left - 1)

        return BarReading(
            left = left,
            hpWidth = hpRight - left + 1,
            mpWidth = mpRight - left + 1,
            hpRows = hpRows,
            mpRows = mpRows,
        )
    }

    /** 막대 가운데 밝은 부분 (255,77,0), (231,65,0) 등. 옆 테두리의 주황/갈색은 파란 성분이 있어서 빠진다 */
    private fun isHpColor(rgb: Int): Boolean {
        val r = rgb shr 16 and 0xFF
        val g = rgb shr 8 and 0xFF
        val b = rgb and 0xFF
        return r >= 170 && b <= 15 && g * 2 <= r
    }

    /** (123,146,189), (82,101,156) 등. 위쪽 능력치 줄의 하늘색 숫자도 걸리지만 위치로 걸러낸다 */
    private fun isMpColor(rgb: Int): Boolean {
        val r = rgb shr 16 and 0xFF
        val b = rgb and 0xFF
        return b >= 150 && b - r >= 50
    }

    private fun longestRun(mask: BooleanArray, offset: Int, length: Int): Int {
        var best = 0
        var current = 0
        for (i in 0 until length) {
            current = if (mask[offset + i]) current + 1 else 0
            if (current > best) best = current
        }
        return best
    }

    /** 줄 범위 안에서 30% 이상 막대 색인 열 */
    private fun filledColumns(mask: BooleanArray, width: Int, rows: IntRange): BooleanArray {
        val need = maxOf(1, (rows.count() * 0.3).toInt())
        return BooleanArray(width) { x -> rows.count { y -> mask[y * width + x] } >= need }
    }

    /** [left]부터 오른쪽으로, [gap]보다 짧은 빈칸(겹친 숫자)은 건너뛰며 막대 끝을 찾는다 */
    private fun filledRight(columns: BooleanArray, left: Int, gap: Int): Int {
        var right = left - 1
        var x = left
        while (x < columns.size) {
            if (columns[x]) right = x
            else if (x - right > gap) break
            x++
        }
        return right
    }
}
