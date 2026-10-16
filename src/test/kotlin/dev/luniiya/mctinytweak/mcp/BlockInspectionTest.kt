package dev.luniiya.mctinytweak.mcp

import net.minecraft.core.BlockPos
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BlockInspectionTest {
    private val air = BlockInspection.Block("minecraft:air", true, null, false, "")

    @Test
    fun `unknown cells are preserved when ordinary air is omitted`() {
        val unknown = BlockPos(1, 0, 0)
        val cells = BlockInspection.inspect(BlockPos.ZERO, 1, false) { if (it == unknown) null else air }
        assertEquals(listOf(BlockInspection.Cell(unknown, null)), cells)
    }

    @Test
    fun `fluid hazards survive air filtering and retain source information`() {
        val lava = BlockInspection.Block("minecraft:lava", false, "minecraft:lava", true, "level=0")
        val fluidInAir = air.copy(fluid = "minecraft:water", fluidSource = true)
        val cells = BlockInspection.inspect(BlockPos.ZERO, 1, false) {
            when (it) { BlockPos.ZERO -> lava; BlockPos(0, 1, 0) -> fluidInAir; else -> air }
        }
        assertEquals(2, cells.size)
        assertEquals(lava, cells.single { it.position == BlockPos.ZERO }.block)
        assertEquals(fluidInAir, cells.single { it.position.y == 1 }.block)
    }

    @Test
    fun `inspection is bounded and invalid radii cannot read the world`() {
        val center = BlockPos(326, -55, 71)
        assertEquals(listOf(center), BlockInspection.inspect(center, 0, true) { air }.map { it.position })
        assertEquals(729, BlockInspection.inspect(center, 4, true) { air }.size)
        for (radius in listOf(-1, 5, Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) {
                BlockInspection.inspect(center, radius, true) { fail("Invalid scan read the world") }
            }
        }
    }
}
