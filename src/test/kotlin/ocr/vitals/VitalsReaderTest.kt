package ocr.vitals

import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** vitals_full.png: 체력/마력이 가득 찬 실제 게임 화면의 우하단 막대 부분 (막대 폭 약 273px) */
class VitalsReaderTest {
    private fun load(): BufferedImage =
        ImageIO.read(javaClass.getResource("/vitals_full.png")).let { source ->
            BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB).also {
                it.graphics.drawImage(source, 0, 0, null)
            }
        }

    /** [percent]만 남기고 그 오른쪽의 마력 막대 색을 어두운 색으로 칠한다 */
    private fun BufferedImage.drainMana(left: Int, fullWidth: Int, percent: Int): BufferedImage {
        val from = left + fullWidth * percent / 100
        for (y in 0 until height) for (x in from until width) {
            val rgb = getRGB(x, y)
            val r = rgb shr 16 and 0xFF
            val b = rgb and 0xFF
            if (b >= 150 && b - r >= 50) setRGB(x, y, 0x0A0A1E)
        }
        return this
    }

    @Test
    fun `가득 찬 막대는 체력과 마력 폭이 같다`() {
        val reading = assertNotNull(VitalsReader.read(load()))
        assertTrue(reading.hpWidth in 265..280, "hp ${reading.hpWidth}")
        assertTrue(reading.mpWidth in 265..280, "mp ${reading.mpWidth}")
        assertNotNull(reading.mpRows)
    }

    @Test
    fun `마력이 줄면 그만큼 퍼센트가 준다`() {
        val full = assertNotNull(VitalsReader.read(load()))
        val gauge = VitalsGauge()
        assertEquals(100 to 100, gauge.percents(full))

        for (percent in listOf(50, 20, 10, 5)) {
            val image = load().drainMana(full.left, full.hpWidth, percent)
            val (hp, mp) = gauge.percents(assertNotNull(VitalsReader.read(image)))
            assertEquals(100, hp)
            assertTrue(mp in percent - 2..percent + 2, "expected about $percent%, got $mp%")
        }
    }

    @Test
    fun `마력이 비면 0퍼센트`() {
        val full = assertNotNull(VitalsReader.read(load()))
        val gauge = VitalsGauge().apply { percents(full) }
        val empty = assertNotNull(VitalsReader.read(load().drainMana(full.left, full.hpWidth, 0)))
        assertEquals(0, gauge.percents(empty).second)
    }
}
