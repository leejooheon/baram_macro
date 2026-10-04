package ocr.vitals

import kotlin.math.roundToInt

/**
 * 막대가 찬 폭을 %로 바꾼다. 막대 전체 길이는 화면에 따로 표시되지 않으므로,
 * 지금까지 본 가장 긴 막대(체력이든 마력이든, 두 막대는 길이가 같다)를 100%로 삼는다.
 * 한 번이라도 체력이나 마력이 가득 찬 걸 보면 정확해진다.
 */
class VitalsGauge {
    /** 지금까지 본 가장 긴 막대 폭(px). 영역을 바꾸면 처음부터 다시 잰다 */
    var fullWidth: Int = 0
        private set

    fun reset() {
        fullWidth = 0
    }

    /** 체력/마력 % 쌍 */
    fun percents(reading: VitalsReader.BarReading): Pair<Int, Int> {
        fullWidth = maxOf(fullWidth, reading.hpWidth, reading.mpWidth)
        return percent(reading.hpWidth) to percent(reading.mpWidth)
    }

    private fun percent(width: Int): Int =
        if (fullWidth <= 0) 0 else (width * 100.0 / fullWidth).roundToInt().coerceIn(0, 100)
}
