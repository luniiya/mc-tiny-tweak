package dev.luniiya.mctinytweak.hotbar

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HotbarHintsTest {
    @Test
    fun `the first four slots hint axe, pickaxe, water bucket and blocks`() {
        assertEquals(
            listOf(HotbarHints.Hint.AXE, HotbarHints.Hint.PICKAXE, HotbarHints.Hint.WATER_BUCKET, HotbarHints.Hint.BLOCK),
            (0 until HotbarLayout.GROUP_SIZE).map(HotbarHints::forSlot),
        )
    }

    @Test
    fun `slots after the first group have no hint`() {
        assertTrue((HotbarLayout.GROUP_SIZE until HotbarLayout.SLOTS).all { HotbarHints.forSlot(it) == null })
    }

    @Test
    fun `hints are faint shadows rather than solid shapes`() {
        val alphas = HotbarHints.FACE_COLORS.values + HotbarHints.SHADOW_COLOR
        for (argb in alphas) {
            assertEquals(0, argb and 0xFFFFFF, "shadows are black")
            assertTrue((argb ushr 24) in 1..0x80, "alpha ${argb ushr 24} stays faint")
        }
    }

    @Test
    fun `the cube fits a slot and is left-right symmetric in shape`() {
        assertEquals(16, HotbarHints.CUBE.size)
        for (line in HotbarHints.CUBE) {
            assertEquals(16, line.length)
            assertTrue(line.all { it == '.' || it in HotbarHints.FACE_COLORS })
            val shape = line.map { it != '.' }
            assertEquals(shape, shape.reversed(), line)
        }
    }

    @Test
    fun `cube runs cover every face pixel once at the given offset`() {
        val runs = HotbarHints.cubeRuns(100, 50)
        val pixels = runs.flatMap { run -> (run.x0 until run.x1).map { it to run.y } }
        val expected = HotbarHints.CUBE.withIndex().flatMap { (y, line) ->
            line.withIndex().filter { it.value != '.' }.map { (x, _) -> 100 + x to 50 + y }
        }
        assertEquals(expected.toSet(), pixels.toSet())
        assertEquals(pixels.size, pixels.toSet().size)
        assertTrue(runs.all { it.argb == HotbarHints.FACE_COLORS.getValue(HotbarHints.CUBE[it.y - 50][it.x0 - 100]) })
    }
}
