package ocr

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ocr.model.Direction

/**
 * 내 캐릭터 상태 저장소. [TimerMonitor]가 매 틱 맵 화면과 좌표 줄을 읽어 여기에 넣고,
 * 매크로는 [state]의 fresh(...)로 최근 값만 꺼내 쓴다.
 */
object CharacterStateHolder {
    private val _state = MutableStateFlow(CharacterState())
    val state: StateFlow<CharacterState> = _state.asStateFlow()

    fun update(state: CharacterState) {
        _state.value = state
    }
}

data class CharacterState(
    /** 우하단에 나오는 게임 좌표 x. 못 읽었으면 null */
    val mapX: Int? = null,
    /** 우하단에 나오는 게임 좌표 y. 못 읽었으면 null */
    val mapY: Int? = null,
    /** 맵 화면에서 찾은 캐릭터 위치. 맵 영역 대비 비율(0~1, 왼쪽 위가 0). 못 찾았으면 null */
    val screenX: Double? = null,
    val screenY: Double? = null,
    /** 등록한 몬스터가 붙어 있는 방향. 캐릭터를 못 찾았거나 몬스터를 등록하지 않았으면 비어 있다 */
    val adjacent: Set<Direction> = emptySet(),
    /** 맵 화면에 보이는 등록한 몬스터 수 (붙은 것 포함) */
    val visibleMonsters: Int = 0,
    val capturedAt: Long = 0,
) {
    /** 내 주변 네 칸 중 몬스터가 붙은 칸 수 (0~4) */
    val adjacentCount: Int get() = adjacent.size

    val found: Boolean get() = screenX != null

    fun isFresh(now: Long = System.currentTimeMillis()): Boolean = now - capturedAt <= MAX_AGE_MILLIS

    companion object {
        /** 이보다 오래된 값은 믿지 않는다 (OCR 주기 1초 + 여유) */
        const val MAX_AGE_MILLIS = 5_000L
    }
}

/** 최근에 읽은 캐릭터 상태. 오래됐으면 null */
fun StateFlow<CharacterState>.fresh(now: Long = System.currentTimeMillis()): CharacterState? =
    value.takeIf { it.isFresh(now) }
