package ocr.character

import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 실제 게임 화면에서 자른 이미지.
 * - character_portrait.png: 장비창 가운데 캐릭터 그림
 * - character_field.png: 맵 일부. 캐릭터는 (363, 285) 근처에 아래를 보고 서 있다.
 */
class CharacterLocatorTest {
    private fun load(name: String): BufferedImage =
        ImageIO.read(javaClass.getResource("/$name")).let { source ->
            BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB).also {
                it.graphics.drawImage(source, 0, 0, null)
            }
        }

    @Test
    fun `맵에서 내 캐릭터를 찾는다`() {
        val reading = assertNotNull(CharacterLocator.locate(load("character_portrait.png"), load("character_field.png")))
        val center = reading.center
        assertTrue(abs(center.x - 363) <= 15 && abs(center.y - 285) <= 15, "found at $center, score ${reading.score}")
        assertTrue(reading.score >= 0.7, "score ${reading.score}")
    }

    @Test
    fun `캐릭터를 지우면 못 찾는다`() {
        val field = load("character_field.png")
        // 캐릭터 자리를 바로 왼쪽 풀밭으로 덮는다
        field.graphics.drawImage(field.getSubimage(150, 200, 120, 160), 300, 200, null)
        assertNull(CharacterLocator.locate(load("character_portrait.png"), field))
    }

    @Test
    fun `등록한 몬스터가 없으면 옆 네 칸이 비어 있다`() {
        val field = load("character_field.png")
        val character = assertNotNull(CharacterLocator.locate(load("character_portrait.png"), field))
        val monsters = AdjacentMonsterDetector.detect(field, character, tileSize = 72, monsters = emptyList())
        assertTrue(monsters.occupied.isEmpty(), "ratios ${monsters.ratios}")
    }
}
