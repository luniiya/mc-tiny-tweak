package dev.luniiya.mctinytweak.mcp

import net.minecraft.core.BlockPos
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WaterPlacementSafetyTest {
    @Test
    fun `Sol's water placement below an adjacent lava cascade is refused`() {
        val destination = BlockPos(323, -52, 71)
        val lava = BlockPos(324, -51, 72)
        assertEquals(listOf(lava), WaterPlacementSafety.unsafeAbove(destination) {
            if (it == lava) WaterPlacementSafety.Cell.LAVA else WaterPlacementSafety.Cell.CLEAR
        })
    }

    @Test
    fun `water above an ordinary flat lava pool remains allowed`() {
        val destination = BlockPos(10, 64, 10)
        assertTrue(WaterPlacementSafety.unsafeAbove(destination) {
            if (it.y < destination.y) WaterPlacementSafety.Cell.LAVA else WaterPlacementSafety.Cell.CLEAR
        }.isEmpty())
    }

    @Test
    fun `unknown cells cannot certify the placement safe`() {
        val unknown = BlockPos(0, 3, 0)
        assertEquals(listOf(unknown), WaterPlacementSafety.unsafeAbove(BlockPos.ZERO) {
            if (it == unknown) WaterPlacementSafety.Cell.UNKNOWN else WaterPlacementSafety.Cell.CLEAR
        })
    }
}
