package ocr.model

/**
 * 맵 화면에서 찾은 내 캐릭터 위치.
 * [x]/[y]는 맵 영역 대비 비율(0~1, 왼쪽 위가 0), [dx]/[dy]는 맵 가운데에서 떨어진 픽셀 (오른쪽/아래가 +).
 */
data class CharacterPosition(
    val x: Double,
    val y: Double,
    val dx: Int,
    val dy: Int,
    /** 장비창 그림 대비 찾은 곳의 캐릭터 색 비율 */
    val score: Double,
    val capturedAt: Long,
) {
    fun isFresh(now: Long = System.currentTimeMillis()): Boolean = now - capturedAt <= MAX_AGE_MILLIS

    companion object {
        /** 이보다 오래된 값은 믿지 않는다 (OCR 주기 1초 + 여유) */
        const val MAX_AGE_MILLIS = 5_000L
    }
}
