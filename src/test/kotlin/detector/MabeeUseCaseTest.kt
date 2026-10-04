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
            Detection(HWND(), 2000, 1200, box(Tile(0, 0)), tiles.map { box(it) }, capturedAt = NOW, latencyMillis = 0)
        },
        now = { NOW },
    )

    @Test
    fun `가까운 몹부터 마비를 건다`() {
        assertEquals(Tile(1, 1), useCase(emptySet(), Tile(3, 0), Tile(1, 1)).plan(NOW)?.first)
    }

    @Test
    fun `5매각에 쓰는 몹과 먼 몹은 건너뛴다`() {
        assertNull(useCase(setOf(Tile(1, 1)), Tile(1, 1), Tile(10, 0)).plan(NOW))
    }

    companion object {
        const val NOW = 1_000_000L
    }
}
