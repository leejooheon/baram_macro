package ocr.capture

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.platform.win32.GDI32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HBITMAP
import com.sun.jna.platform.win32.WinDef.HDC
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinGDI
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import kotlin.math.roundToInt

/**
 * 게임 창(HWND)만 직접 캡처한다.
 *
 * - 다른 창이 위에 덮여 있어도 찍힌다 (PrintWindow + PW_RENDERFULLCONTENT).
 * - 창의 클라이언트 영역을 실제 픽셀 해상도로 찍으므로 윈도우 배율(150% 등)과 상관없다.
 * - 좌표가 게임 창 기준이라 창을 옮겨도 영역을 다시 잡을 필요가 없다.
 */
object GameWindowCapture {
    private const val PW_CLIENTONLY = 0x1
    private const val PW_RENDERFULLCONTENT = 0x2
    private const val SRCCOPY = 0x00CC0020

    data class GameWindow(val hwnd: HWND, val title: String)

    /** 제목에 [keyword]가 들어간 보이는 창을 찾는다. */
    fun find(keyword: String): GameWindow? {
        val user32 = User32.INSTANCE
        var found: GameWindow? = null
        user32.EnumWindows({ hwnd, _ ->
            if (!user32.IsWindowVisible(hwnd)) return@EnumWindows true
            val buffer = CharArray(512)
            user32.GetWindowText(hwnd, buffer, buffer.size)
            val title = Native.toString(buffer)
            if (keyword.isNotBlank() && title.contains(keyword)) {
                found = GameWindow(hwnd, title)
                false
            } else {
                true
            }
        }, null)
        return found
    }
    fun isAlive(window: GameWindow): Boolean = User32.INSTANCE.IsWindow(window.hwnd)

    fun setForeground(window: GameWindow) {
        User32.INSTANCE.SetForegroundWindow(window.hwnd)
    }

    /** 게임 창 클라이언트 영역 전체 (영역 지정 화면용). 창이 최소화돼 있으면 null */
    fun capture(window: GameWindow): BufferedImage? =
        withWindowImage(window) { size, read -> read(Rectangle(0, 0, size.width, size.height)) }

    data class RegionCapture(val windowSize: Dimension, val images: List<BufferedImage>)

    /**
     * 게임 창에서 [regions](창 대비 비율) 부분만 읽어 온다.
     * 창 전체(수 MB)를 자바 이미지로 옮기지 않으므로 매 틱 캡처 비용이 훨씬 작다.
     */
    fun captureRegions(window: GameWindow, regions: List<Rectangle2D.Double>): RegionCapture? =
        withWindowImage(window) { size, read ->
            val bounds = Rectangle(0, 0, size.width, size.height)
            val images = regions.map { fraction ->
                val rect = Rectangle(
                    (fraction.x * size.width).roundToInt(),
                    (fraction.y * size.height).roundToInt(),
                    (fraction.width * size.width).roundToInt().coerceAtLeast(1),
                    (fraction.height * size.height).roundToInt().coerceAtLeast(1),
                ).intersection(bounds)
                if (rect.isEmpty) return@withWindowImage null
                read(rect) ?: return@withWindowImage null
            }
            RegionCapture(size, images)
        }

    /** PrintWindow가 검은 화면을 주는 창이면 다음부터는 바로 화면 복사를 쓴다 */
    private var printWindowGivesBlack = false

    private fun <T> withWindowImage(
        window: GameWindow,
        block: (size: Dimension, read: (Rectangle) -> BufferedImage?) -> T?,
    ): T? {
        val user32 = User32.INSTANCE
        val gdi32 = GDI32.INSTANCE

        val rect = RECT()
        if (!user32.GetClientRect(window.hwnd, rect)) return null
        val width = rect.right - rect.left
        val height = rect.bottom - rect.top
        if (width <= 0 || height <= 0) return null
        val size = Dimension(width, height)

        val windowDc = user32.GetDC(window.hwnd) ?: return null
        val memoryDc = gdi32.CreateCompatibleDC(windowDc)
        val bitmap = gdi32.CreateCompatibleBitmap(windowDc, width, height)
        val previous = gdi32.SelectObject(memoryDc, bitmap)
        try {
            val read: (Rectangle) -> BufferedImage? = { r -> copyRegion(windowDc, memoryDc, r) }
            if (!printWindowGivesBlack) {
                val printed = user32.PrintWindow(window.hwnd, memoryDc, PW_CLIENTONLY or PW_RENDERFULLCONTENT)
                if (printed) {
                    val result = block(size, read)
                    if (!isBlackResult(result)) return result
                    // 일부 DirectX 창은 PrintWindow가 검은 화면을 준다. 그때는 화면에 보이는 그대로 복사한다
                    printWindowGivesBlack = true
                }
            }
            blitFromScreen(memoryDc, windowDc, width, height)
            return block(size, read)
        } finally {
            gdi32.SelectObject(memoryDc, previous)
            gdi32.DeleteObject(bitmap)
            gdi32.DeleteDC(memoryDc)
            user32.ReleaseDC(window.hwnd, windowDc)
        }
    }

    private fun isBlackResult(result: Any?): Boolean = when (result) {
        is BufferedImage -> result.isBlack()
        is RegionCapture -> result.images.all { it.isBlack() }
        else -> false
    }

    /** 메모리 DC의 [rect] 부분만 작은 비트맵으로 복사해서 읽는다 */
    private fun copyRegion(windowDc: HDC, sourceDc: HDC, rect: Rectangle): BufferedImage? {
        val gdi32 = GDI32.INSTANCE
        val regionDc = gdi32.CreateCompatibleDC(windowDc)
        val regionBitmap = gdi32.CreateCompatibleBitmap(windowDc, rect.width, rect.height)
        val previous = gdi32.SelectObject(regionDc, regionBitmap)
        try {
            gdi32.BitBlt(regionDc, 0, 0, rect.width, rect.height, sourceDc, rect.x, rect.y, SRCCOPY)
            return readBitmap(windowDc, regionDc, regionBitmap, previous, rect.width, rect.height)
        } finally {
            gdi32.SelectObject(regionDc, previous)
            gdi32.DeleteObject(regionBitmap)
            gdi32.DeleteDC(regionDc)
        }
    }

    /** 창이 가려져 있으면 가린 창이 찍힌다 */
    private fun blitFromScreen(memoryDc: HDC, windowDc: HDC, width: Int, height: Int) {
        GDI32.INSTANCE.BitBlt(memoryDc, 0, 0, width, height, windowDc, 0, 0, SRCCOPY)
    }

    private fun readBitmap(
        windowDc: HDC,
        memoryDc: HDC,
        bitmap: HBITMAP,
        previous: WinNT.HANDLE,
        width: Int,
        height: Int,
    ): BufferedImage? {
        val gdi32 = GDI32.INSTANCE
        val info = WinGDI.BITMAPINFO().apply {
            bmiHeader.biWidth = width
            bmiHeader.biHeight = -height // 위에서 아래 순서
            bmiHeader.biPlanes = 1
            bmiHeader.biBitCount = 32
            bmiHeader.biCompression = WinGDI.BI_RGB
        }
        val buffer = Memory(width.toLong() * height * 4)
        // GetDIBits 전에 비트맵을 DC에서 빼야 한다
        gdi32.SelectObject(memoryDc, previous)
        if (gdi32.GetDIBits(windowDc, bitmap, 0, height, buffer, info, WinGDI.DIB_RGB_COLORS) == 0) return null

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val pixels = (image.raster.dataBuffer as DataBufferInt).data
        buffer.read(0, pixels, 0, pixels.size)
        return image
    }

    private fun BufferedImage.isBlack(): Boolean {
        for (i in 1..9) for (j in 1..9) {
            if (getRGB(width * i / 10, height * j / 10) and 0xFFFFFF != 0) return false
        }
        return true
    }
}
