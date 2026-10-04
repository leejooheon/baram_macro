package ocr

import common.robot.Keyboard
import java.awt.Rectangle
import java.awt.image.BufferedImage

object NativeScreenCapture {
    /**
     * 논리 좌표 영역을 실제 화면 픽셀 해상도로 캡처한다.
     * Robot.createScreenCapture는 윈도우 배율(예: 150%)만큼 줄인 이미지를 줘서 글자가 뭉개지므로,
     * 여러 해상도 캡처 중 가장 큰 것을 쓴다.
     */
    fun capture(rectangle: Rectangle): BufferedImage {
        val multi = Keyboard.robot.createMultiResolutionScreenCapture(rectangle)
        val best = multi.resolutionVariants.maxBy { it.getWidth(null) * it.getHeight(null) }
        if (best is BufferedImage) return best

        val width = best.getWidth(null)
        val height = best.getHeight(null)
        return BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).also {
            val graphics = it.createGraphics()
            graphics.drawImage(best, 0, 0, null)
            graphics.dispose()
        }
    }

    /** 영역 지정 화면에 깔 전체 화면 (논리 해상도) */
    fun captureFullScreen(): BufferedImage {
        val size = java.awt.Toolkit.getDefaultToolkit().screenSize
        return Keyboard.robot.createScreenCapture(Rectangle(0, 0, size.width, size.height))
    }
}
