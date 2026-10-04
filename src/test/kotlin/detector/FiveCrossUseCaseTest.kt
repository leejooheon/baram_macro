package detector

import com.sun.jna.platform.win32.WinDef.HWND
import jusulsa.usecase.FiveCrossUseCase
import jusulsa.usecase.FiveCrossUseCase.Action
import ocr.OcrStateHolder
import ocr.RegionResult
import ocr.model.TimerRegion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class FiveCrossUseCaseTest {
    private val tile = 40
    private val me = Tile(0, 0)

    /** 나는 화면 (500, 300), 한 칸 40px */
    private fun detection(vararg tiles: Tile) = Detection(
        window = HWND(),
        windowWidth = 1000,
        windowHeight = 600,
        me = box(me),
        monsters = tiles.map { box(it) },
        capturedAt = NOW,
        latencyMillis = 0,
    )

    private fun box(t: Tile) = java.awt.Rectangle(500 + t.x * tile - tile / 2, 300 + t.y * tile - tile / 2, tile, tile)

    private fun useCase(vararg tiles: Tile) = FiveCrossUseCase(detection = { detection(*tiles) }, now = { NOW })

    @Test
    fun `칸 좌표로 바꾼다`() {
        val d = detection(Tile(3, -2))
        assertEquals(Tile(3, -2), TileGrid.of(d)!!.tileOf(d.monsters.single()))
    }

    @Test
    fun `세로로 긴 캐릭터 박스도 발 위치로 칸을 잡는다`() {
        // 칸은 40px 정사각형, 그림은 70px 높이로 칸 위로 삐져나온다
        fun tall(t: Tile) = java.awt.Rectangle(500 + t.x * tile - tile / 2, 300 + t.y * tile + tile / 2 - 70, tile, 70)
        val d = Detection(HWND(), 2000, 1200, tall(Tile(0, 0)), listOf(tall(Tile(0, 1)), tall(Tile(0, -2))), capturedAt = NOW, latencyMillis = 0)
        val grid = TileGrid.of(d)!!
        assertEquals(listOf(Tile(0, 1), Tile(0, -2)), d.monsters.map { grid.tileOf(it) })
    }

    @Test
    fun `상하좌우가 다 차면 중심에 삼매진화`() {
        OcrStateHolder.update(TimerRegion.COOLDOWN, RegionResult(emptyList(), capturedAt = NOW, success = true))
        val c = Tile(3, 0)
        val action = useCase(c, *c.neighbors.toTypedArray()).plan(NOW)
        assertIs<Action.Samme>(action)
        assertEquals(c, action.center)
    }

    @Test
    fun `각이 덜 찼으면 중심은 두고 붙은 몹부터 묶는다`() {
        val c = Tile(3, 0)
        val action = useCase(c, Tile(3, -1), Tile(4, 0)).plan(NOW)
        assertIs<Action.Hold>(action)
        assertEquals(Tile(3, -1), action.tile)
    }

    @Test
    fun `붙은 몹이 적으면 아무것도 안 한다`() {
        assertNull(useCase(Tile(3, 0), Tile(-3, 0)).plan(NOW))
    }

    companion object {
        const val NOW = 1_000_000L
    }
}
