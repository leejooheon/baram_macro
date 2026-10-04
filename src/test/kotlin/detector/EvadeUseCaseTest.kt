package detector

import com.sun.jna.platform.win32.WinDef.HWND
import jusulsa.usecase.EvadeUseCase
import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EvadeUseCaseTest {
    private val tile = 40

    private fun box(t: Tile) = Rectangle(500 + t.x * tile - tile / 2, 300 + t.y * tile - tile / 2, tile, tile)

    private fun useCase(vararg tiles: Tile, ignore: Set<Tile> = emptySet()) = EvadeUseCase(
        ignore = { ignore },
        detection = {
            Detection(HWND(), windowWidth(tile), 1200, box(Tile(0, 0)), tiles.map { box(it) }, capturedAt = NOW, latencyMillis = 0)
        },
        now = { NOW },
    )

    @Test
    fun `옆에 몹이 없으면 안 움직인다`() {
        assertNull(useCase(Tile(3, 0)).plan(NOW))
    }

    @Test
    fun `두 칸 앞까지 다가오면 미리 피한다`() {
        assertEquals(Tile(-1, 0), useCase(Tile(2, 0)).plan(NOW))
    }

    @Test
    fun `오른쪽에 붙으면 왼쪽으로 피한다`() {
        assertEquals(Tile(-1, 0), useCase(Tile(1, 0)).plan(NOW))
    }

    @Test
    fun `몹이 적게 붙는 쪽으로 피한다`() {
        // 오른쪽, 아래에 붙어 있고 왼쪽 칸 옆(-2,0)에도 몹이 있으면 위로
        assertEquals(Tile(0, -1), useCase(Tile(1, 0), Tile(0, 1), Tile(-2, 0)).plan(NOW))
    }

    @Test
    fun `사방이 막혔으면 안 움직인다`() {
        assertNull(useCase(Tile(1, 0), Tile(-1, 0), Tile(0, 1), Tile(0, -1)).plan(NOW))
    }

    @Test
    fun `마비 걸린 몹은 무시한다`() {
        assertNull(useCase(Tile(1, 0), ignore = setOf(Tile(1, 0))).plan(NOW))
    }

    companion object {
        const val NOW = 1_000_000L
    }
}
