package ocr.model

/** 체력/마력 막대가 찬 비율 (0~100) */
data class Vitals(
    val hpPercent: Int,
    val mpPercent: Int,
    val capturedAt: Long,
) {
    fun isFresh(now: Long = System.currentTimeMillis()): Boolean = now - capturedAt <= MAX_AGE_MILLIS

    companion object {
        /** 이보다 오래된 값은 믿지 않는다 (OCR 주기 1초 + 여유) */
        const val MAX_AGE_MILLIS = 5_000L
    }
}
