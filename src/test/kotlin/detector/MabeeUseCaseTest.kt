package detector

import com.sun.jna.platform.win32.WinDef.HWND
import jusulsa.usecase.MabeeUseCase
import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MabeeUseCaseTest {
    private val tile = 40

    private fun box(t: Tile) = Rectangle(500 + t.x * tile - tile / 2, 300 + t.y * tile - tile / 2, tile, tile)

    private fun useCase(reserved: Set<Tile>, vararg tiles: Tile) = MabeeUseCase(
        reserved = { reserved },
        detection = {
            Detection(HWND(), windowWidth(tile), 1200, box(Tile(0, 0)), tiles.map { box(it) }, capturedAt = NOW, latencyMillis = 0)
        },
        now = { NOW },
    )

    @Test
    fun `내 옆에 붙은 몹에만 마비를 건다`() {
        assertEquals(Tile(0, 1), useCase(emptySet(), Tile(3, 0), Tile(1, 1), Tile(0, 1)).plan(NOW)?.first)
    }

    @Test
    fun `대각선이나 먼 몹, 내 칸은 찍지 않는다`() {
        assertNull(useCase(emptySet(), Tile(1, 1), Tile(3, 0), Tile(0, 0)).plan(NOW))
    }

    @Test
    fun `5매각에 쓰는 몹은 건너뛴다`() {
        assertNull(useCase(setOf(Tile(1, 0)), Tile(1, 0)).plan(NOW))
    }

    companion object {
        const val NOW = 1_000_000L
    }
}
