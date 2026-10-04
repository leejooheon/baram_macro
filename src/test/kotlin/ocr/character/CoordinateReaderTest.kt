package ocr.character

import java.awt.Image
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** coords_0039_0144.png: 실제 게임 화면 우하단 좌표 줄 (2554px 폭 창) */
class CoordinateReaderTest {
    private fun load(scale: Double = 1.0): BufferedImage =
        ImageIO.read(javaClass.getResource("/coords_0039_0144.png")).let { source ->
            val w = (source.width * scale).toInt()
            val h = (source.height * scale).toInt()
            BufferedImage(w, h, BufferedImage.TYPE_INT_RGB).also {
                it.graphics.drawImage(source.getScaledInstance(w, h, Image.SCALE_REPLICATE), 0, 0, null)
            }
        }

    @Test
    fun `좌표 줄을 읽는다`() {
        assertEquals(CoordinateReader.Coordinate(39, 144), CoordinateReader.read(load()))
    }

    @Test
    fun `창이 작아져도 읽는다`() {
        assertEquals(CoordinateReader.Coordinate(39, 144), CoordinateReader.read(load(scale = 2.0 / 3)))
    }

    @Test
    fun `영역 끝에 걸린 패널 테두리 조각은 무시한다`() {
        val source = load()
        // 오른쪽에 숫자색 가로줄 세 개(테두리 조각)를 붙인다
        val image = BufferedImage(source.width + 30, source.height, BufferedImage.TYPE_INT_RGB)
        val g = image.graphics
        g.drawImage(source, 0, 0, null)
        g.color = java.awt.Color(247, 227, 156)
        listOf(8, 14, 32).forEach { y -> g.fillRect(source.width + 6, y, 21, 4) }
        assertEquals(CoordinateReader.Coordinate(39, 144), CoordinateReader.read(image))
    }
}
