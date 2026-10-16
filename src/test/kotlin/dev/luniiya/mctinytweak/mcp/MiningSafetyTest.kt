package dev.luniiya.mctinytweak.mcp

import net.minecraft.core.Direction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MiningSafetyTest {
    @Test
    fun `digging a ceiling under lava is refused even when standing near water`() {
        assertTrue(MiningSafety.exposesLava(setOf(Direction.UP), false))
        assertTrue(MiningSafety.exposesLava(setOf(Direction.UP), true))
    }

    @Test
    fun `obsidian over lava needs water above before direct mining`() {
        assertTrue(MiningSafety.exposesLava(setOf(Direction.DOWN), false))
        assertFalse(MiningSafety.exposesLava(setOf(Direction.DOWN), true))
    }

    @Test
    fun `all unprotected lava faces are refused but ordinary mining stays available`() {
        for (direction in Direction.entries) assertTrue(MiningSafety.exposesLava(setOf(direction), false))
        assertFalse(MiningSafety.exposesLava(emptySet(), false))
        assertFalse(MiningSafety.exposesLava(emptySet(), true))
    }
}
