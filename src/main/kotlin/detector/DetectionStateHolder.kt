package detector

import com.sun.jna.platform.win32.WinDef.HWND
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.Point
import java.awt.Rectangle

/**
 * 몹/내 캐릭터 탐지 결과 저장소. [DetectionMonitor]가 넣고, UseCase는 이 값만 보고 대상을 고른다.
 * 좌표는 모두 게임 창 클라이언트 영역 픽셀 기준이라 [common.robot.Mouse.click]에 그대로 넘기면 된다.
 */
object DetectionStateHolder {
    private val _state = MutableStateFlow<Detection?>(null)
    val state: StateFlow<Detection?> = _state.asStateFlow()

    fun update(detection: Detection) {
        _state.value = detection
    }

    /** 최근 탐지 결과에서 나와 가장 가까운 몹. 오래됐거나 몹이 없으면 null */
    fun nearestMonster(now: Long = System.currentTimeMillis()): Aim? {
        val detection = state.value?.takeIf { it.isFresh(now) } ?: return null
        val monster = detection.nearestMonster() ?: return null
        return Aim(detection.window, monster.center())
    }
}

/** 클릭할 게임 창과 그 안의 점 */
data class Aim(val window: HWND, val point: Point)

data class Detection(
    val window: HWND,
    val windowWidth: Int,
    val windowHeight: Int,
    /** 내 캐릭터. 못 찾았으면 null */
    val me: Rectangle?,
    val monsters: List<Rectangle>,
    val capturedAt: Long,
    /** 캡처부터 결과를 받기까지 걸린 시간 */
    val latencyMillis: Long,
) {
    /** 내 캐릭터를 못 찾았으면 화면 가운데에 있다고 본다 (화면이 캐릭터를 따라다닌다) */
    val myPosition: Point get() = me?.center() ?: Point(windowWidth / 2, windowHeight / 2)

    fun nearestMonster(): Rectangle? {
        val me = myPosition
        return monsters.minByOrNull { it.center().distanceSq(me) }
    }

    fun isFresh(now: Long = System.currentTimeMillis()): Boolean = now - capturedAt <= MAX_AGE_MILLIS

    companion object {
        /** 몹은 계속 움직이니 이보다 오래된 위치로는 클릭하지 않는다 */
        const val MAX_AGE_MILLIS = 1_000L
    }
}

fun Rectangle.center() = Point(x + width / 2, y + height / 2)
