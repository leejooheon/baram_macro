package ocr.character

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** 등록한 몬스터 그림을 `~/.baram_macro/monsters/`에 PNG로 저장한다. 저장소에는 올리지 않는다. */
object MonsterStore {
    private val dir = File(System.getProperty("user.home"), ".baram_macro/monsters")

    fun load(): List<BufferedImage> =
        dir.listFiles { file -> file.extension == "png" }
            ?.sortedBy { it.name }
            ?.mapNotNull { file -> runCatching { ImageIO.read(file)?.toRgb() }.getOrNull() }
            .orEmpty()

    fun add(image: BufferedImage) {
        runCatching {
            dir.mkdirs()
            ImageIO.write(image, "png", File(dir, "monster_${System.currentTimeMillis()}.png"))
        }
    }

    fun clear() {
        dir.listFiles { file -> file.extension == "png" }?.forEach { it.delete() }
    }

    private fun BufferedImage.toRgb(): BufferedImage =
        BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).also { it.graphics.drawImage(this, 0, 0, null) }
}
