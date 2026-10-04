package jusulsa.usecase

import ocr.OcrStateHolder
import ocr.model.TimerRegion

/**
 * 헬파이어. 쿨타임 박스에 헬파이어가 없을 때만 쓴다. 대상은 쓰는 쪽(첨첨의 저주)이 잡는다.
 * 쿨타임 박스를 못 읽으면 쿨인지 알 수 없으니 쓰지 않는다.
 */
class HellfireUseCase(
    private val ocr: OcrStateHolder = OcrStateHolder,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var lastCastAt: Long? = null

    fun canCast(): Boolean {
        val time = now()
        // 쓴 직후에는 쿨타임 박스에 아직 안 잡힌다
        lastCastAt?.let { if (time - it < RECAST_GUARD_MILLIS) return false }

        val cooldown = ocr.state.value.fresh(TimerRegion.COOLDOWN, time) ?: return false
        // OCR이 이름을 조금씩 다르게 읽어서 "헬"만 보고 찾는다
        val entry = cooldown.entries.firstOrNull { NAME_KEY in it.name } ?: return true
        return (entry.remainingSeconds(time) ?: return false) <= 0
    }

    fun onCast() {
        lastCastAt = now()
    }

    companion object {
        const val NAME_KEY = "헬"
        const val RECAST_GUARD_MILLIS = 5_000L
    }
}
