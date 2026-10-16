package dev.luniiya.mctinytweak.mcp

import net.minecraft.core.BlockPos
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NetherPortalPlanTest {
    @Test
    fun `minimum frame has ten obsidian and an open two by three interior`() {
        val p = NetherPortalPlan(BlockPos(350, 70, 35), "x")
        assertEquals(10, p.cells.count { it.role == NetherPortalPlan.Role.OBSIDIAN })
        assertEquals(4, p.cells.count { it.role == NetherPortalPlan.Role.CORNER })
        assertEquals(6, p.cells.count { it.role == NetherPortalPlan.Role.INTERIOR })
        assertEquals(20, p.cells.map { it.position }.distinct().size)
        assertEquals(NetherPortalPlan.Role.OBSIDIAN, p.role(1, 0, 0))
        assertEquals(NetherPortalPlan.Role.INTERIOR, p.role(1, 1, 0))
        assertNull(p.role(1, 1, 1))
        assertNull(p.role(-1, 0, 0))
    }

    @Test
    fun `both orientations keep ignition inside the frame and approach outside`() {
        for (axis in listOf("x", "z")) {
            val p = NetherPortalPlan(BlockPos(-10, 80, -20), axis)
            assertEquals(NetherPortalPlan.Role.OBSIDIAN, p.cells.single { it.position == p.ignitionBlock }.role)
            assertEquals(NetherPortalPlan.Role.INTERIOR, p.cells.single { it.position == p.ignitionBlock.above() }.role)
            assertFalse(p.cells.any { it.position == p.approach })
        }
        val x = NetherPortalPlan(BlockPos.ZERO, "x")
        val z = NetherPortalPlan(BlockPos.ZERO, "z")
        assertEquals(x.cells.map { it.position.x to it.position.y }, z.cells.map { it.position.z to it.position.y })
    }

    @Test
    fun `invalid orientation and world bounds are rejected before building`() {
        assertThrows(IllegalArgumentException::class.java) { NetherPortalPlan(BlockPos.ZERO, "y") }
        assertThrows(IllegalArgumentException::class.java) { NetherPortalPlan(BlockPos(Int.MAX_VALUE, 0, 0), "x") }
    }
}
