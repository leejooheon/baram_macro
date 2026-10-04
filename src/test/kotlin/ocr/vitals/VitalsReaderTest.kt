package ocr.vitals

import java.awt.Rectangle
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * vitals_full.png: 체력/마력이 가득 찬 실제 게임 화면의 우하단 막대 부분.
 * 빈 막대는 게임에서 (8,4,8)로 보이므로, 막대 색을 그 색으로 칠해서 줄어든 상태를 만든다 (겹친 숫자는 그대로 둔다).
 */
class VitalsReaderTest {
    private fun load(): BufferedImage =
        ImageIO.read(javaClass.getResource("/vitals_full.png")).let { source ->
            BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB).also {
                it.graphics.drawImage(source, 0, 0, null)
            }
        }

    /** [box] 막대를 [percent]만 남기고 비운다. [fromLeft]면 왼쪽부터 빈다 (체력 막대가 이렇게 줄어든다) */
    private fun BufferedImage.drain(box: Rectangle, percent: Int, fromLeft: Boolean): BufferedImage {
        val emptied = (box.width * (100 - percent) / 100.0).roundToInt()
        val columns = if (fromLeft) box.x until box.x + emptied else box.x + box.width - emptied until box.x + box.width
        for (y in box.y - 4..box.y + box.height + 4) for (x in columns) {
            val rgb = getRGB(x, y)
            val r = rgb shr 16 and 0xFF
            val g = rgb shr 8 and 0xFF
            val b = rgb and 0xFF
            val isBar = (b > g && b > 40) || (r > 100 && g < r)
            if (isBar) setRGB(x, y, 0x080408)
        }
        return this
    }

    /** 경계가 숫자 밑에 가려지면 글자 반 폭(약 4%)까지 틀릴 수 있다 */
    private fun assertAbout(expected: Int, actual: Int?) {
        assertNotNull(actual)
        assertTrue(abs(expected - actual) <= 5, "expected about $expected%, got $actual%")
    }

    @Test
    fun `가득 찬 막대는 100퍼센트`() {
        val reading = assertNotNull(VitalsReader.read(load()))
        assertEquals(100, reading.hpPercent)
        assertEquals(100, reading.mpPercent)
        assertTrue(reading.hpBox.width in 265..280, "hp box ${reading.hpBox}")
        assertNotNull(reading.mpBox)
    }

    @Test
    fun `체력이 왼쪽부터 비면 남은 만큼`() {
        val full = assertNotNull(VitalsReader.read(load()))
        for (percent in listOf(52, 20, 10, 5, 0)) {
            val reading = assertNotNull(VitalsReader.read(load().drain(full.hpBox, percent, fromLeft = true)))
            assertAbout(percent, reading.hpPercent)
            assertEquals(100, reading.mpPercent)
        }
    }

    @Test
    fun `마력은 어느 쪽에서 비어도 남은 만큼`() {
        val full = assertNotNull(VitalsReader.read(load()))
        val mpBox = assertNotNull(full.mpBox)
        for (fromLeft in listOf(true, false)) for (percent in listOf(50, 20, 10, 5, 0)) {
            val reading = assertNotNull(VitalsReader.read(load().drain(mpBox, percent, fromLeft)))
            assertEquals(100, reading.hpPercent)
            assertAbout(percent, reading.mpPercent)
        }
    }
}
