package detector

import com.sun.jna.platform.win32.WinDef.HWND
import java.awt.Point
import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DetectionTest {
    private fun detection(me: Rectangle?, vararg monsters: Rectangle, capturedAt: Long = 0) = Detection(
        window = HWND(),
        windowWidth = 1000,
        windowHeight = 600,
        me = me,
        monsters = monsters.toList(),
        capturedAt = capturedAt,
        latencyMillis = 0,
    )

    @Test
    fun `내 캐릭터와 가장 가까운 몹을 고른다`() {
        val far = Rectangle(900, 500, 20, 20)
        val near = Rectangle(120, 110, 20, 20)
        assertEquals(near, detection(Rectangle(100, 100, 20, 20), far, near).nearestMonster())
    }

    @Test
    fun `내 캐릭터를 못 찾으면 화면 가운데를 기준으로 한다`() {
        val center = Rectangle(510, 290, 20, 20)
        val corner = Rectangle(0, 0, 20, 20)
        val detection = detection(null, corner, center)
        assertEquals(Point(500, 300), detection.myPosition)
        assertEquals(center, detection.nearestMonster())
    }

    @Test
    fun `몹이 없으면 대상이 없다`() {
        assertNull(detection(Rectangle(0, 0, 10, 10)).nearestMonster())
    }

    @Test
    fun `오래된 탐지 결과로는 클릭하지 않는다`() {
        DetectionStateHolder.update(detection(null, Rectangle(0, 0, 10, 10), capturedAt = 0))
        assertNull(DetectionStateHolder.nearestMonster(now = Detection.MAX_AGE_MILLIS + 1))
        assertEquals(Point(5, 5), DetectionStateHolder.nearestMonster(now = 100)?.point)
    }
}
