package ocr.character

import java.awt.Point
import java.awt.Rectangle
import java.awt.image.BufferedImage

/**
 * 맵 화면에서 내 캐릭터 위치를 찾는다. OCR 서버를 쓰지 않는다.
 *
 * 장비창 가운데에는 내 캐릭터가 맵과 같은 크기, 같은 옷으로 그려져 있다. 그 그림에서 "캐릭터에만 있는 색"을 골라
 * 맵에서 그 색이 가장 많이 모인 곳을 캐릭터로 본다.
 * - 색 분포만 보므로 캐릭터가 어느 방향을 보든, 옷을 갈아입어도 장비창을 따라 바로 맞춰진다.
 * - 장비창 바탕(테두리 쪽에 보이는 색)과 지금 맵에 흔한 색(풀, 땅 등)은 매번 빼고 고른다.
 */
object CharacterLocator {
    /** 색을 채널마다 이 폭으로 묶는다 (8단계 x 3채널 = 512칸) */
    private const val STEP = 32
    private const val BINS = 512

    /** 장비창 그림에서 이 비율 이상 나오는 색만 캐릭터 색 후보로 본다 */
    private const val MIN_PORTRAIT_SHARE = 0.003
    /** 장비창 테두리 쪽에서 이 비율 이상 보이는 색은 바탕으로 보고 뺀다 */
    private const val MAX_BACKGROUND_SHARE = 0.01
    /** 맵 전체에서 이 비율 이상 보이는 색은 흔해서 뺀다 */
    private const val MAX_FIELD_SHARE = 0.002
    /** 찾은 곳의 캐릭터 색 개수가 장비창 그림 대비 이 비율 이상이어야 캐릭터로 인정한다 */
    const val MIN_SCORE = 0.4

    data class Reading(
        /** 맵 영역 이미지 픽셀 기준 캐릭터 칸 */
        val box: Rectangle,
        /** 장비창 그림 대비 찾은 곳의 캐릭터 색 비율 (1이면 똑같이 많다) */
        val score: Double,
        /** 고른 캐릭터 색 개수 (색 칸 기준) */
        val colorCount: Int,
    ) {
        val center: Point get() = Point(box.x + box.width / 2, box.y + box.height / 2)
    }

    /** [portrait]는 장비창의 캐릭터 그림 영역, [field]는 맵 화면 영역. 못 찾으면 null */
    fun locate(portrait: BufferedImage, field: BufferedImage): Reading? {
        val pw = portrait.width
        val ph = portrait.height
        val fw = field.width
        val fh = field.height
        if (pw < 8 || ph < 8) return null
        val portraitBins = bins(portrait)
        val fieldBins = bins(field)

        val portraitCounts = IntArray(BINS).also { c -> portraitBins.forEach { c[it]++ } }
        val fieldCounts = IntArray(BINS).also { c -> fieldBins.forEach { c[it]++ } }
        val border = maxOf(2, minOf(pw, ph) / 16)
        val borderCounts = IntArray(BINS)
        var borderTotal = 0
        for (y in 0 until ph) for (x in 0 until pw) {
            if (x < border || x >= pw - border || y < border || y >= ph - border) {
                borderCounts[portraitBins[y * pw + x]]++
                borderTotal++
            }
        }

        val isKey = BooleanArray(BINS) { b ->
            portraitCounts[b] >= portraitBins.size * MIN_PORTRAIT_SHARE &&
                borderCounts[b] < borderTotal * MAX_BACKGROUND_SHARE &&
                fieldCounts[b] < fieldBins.size * MAX_FIELD_SHARE
        }
        val colorCount = isKey.count { it }
        if (colorCount == 0) return null

        // 장비창에서 캐릭터 색이 있는 곳의 크기를 맵에서 찾을 칸 크기로 쓴다 (장비창 아이콘 몇 점은 5%/95%로 걸러진다)
        val xs = ArrayList<Int>()
        val ys = ArrayList<Int>()
        for (y in 0 until ph) for (x in 0 until pw) {
            if (isKey[portraitBins[y * pw + x]]) { xs += x; ys += y }
        }
        if (xs.size < 20) return null
        xs.sort(); ys.sort()
        val x0 = xs[xs.size * 5 / 100]
        val x1 = xs[xs.size * 95 / 100]
        val y0 = ys[ys.size * 5 / 100]
        val y1 = ys[ys.size * 95 / 100]
        val w = x1 - x0 + 1
        val h = y1 - y0 + 1
        if (w > fw || h > fh) return null
        var expected = 0
        for (y in y0..y1) for (x in x0..x1) if (isKey[portraitBins[y * pw + x]]) expected++
        if (expected == 0) return null

        // 맵에서 캐릭터 색인 픽셀을 누적합으로 만들어 w x h 칸마다 개수를 센다
        val stride = fw + 1
        val sums = IntArray(stride * (fh + 1))
        for (y in 0 until fh) {
            var row = 0
            for (x in 0 until fw) {
                if (isKey[fieldBins[y * fw + x]]) row++
                sums[(y + 1) * stride + x + 1] = sums[y * stride + x + 1] + row
            }
        }
        var best = -1
        var bestX = 0
        var bestY = 0
        for (y in 0..fh - h) for (x in 0..fw - w) {
            val count = sums[(y + h) * stride + x + w] - sums[y * stride + x + w] -
                sums[(y + h) * stride + x] + sums[y * stride + x]
            if (count > best) { best = count; bestX = x; bestY = y }
        }
        val score = best.toDouble() / expected
        if (score < MIN_SCORE) return null
        return Reading(box = Rectangle(bestX, bestY, w, h), score = score, colorCount = colorCount)
    }

    private fun bins(image: BufferedImage): IntArray {
        val pixels = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
        return IntArray(pixels.size) { i ->
            val rgb = pixels[i]
            val r = (rgb shr 16 and 0xFF) / STEP
            val g = (rgb shr 8 and 0xFF) / STEP
            val b = (rgb and 0xFF) / STEP
            r * 64 + g * 8 + b
        }
    }
}
