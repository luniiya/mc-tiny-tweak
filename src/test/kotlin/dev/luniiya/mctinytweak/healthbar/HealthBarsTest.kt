package dev.luniiya.mctinytweak.healthbar

import dev.luniiya.mctinytweak.healthbar.HealthBars.GOLD
import dev.luniiya.mctinytweak.healthbar.HealthBars.MISSING
import dev.luniiya.mctinytweak.healthbar.HealthBars.Segment
import dev.luniiya.mctinytweak.healthbar.HealthBars.layerColor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HealthBarsTest {
    private val orange = layerColor(0)
    private val red = layerColor(1)
    private val pink = layerColor(2)
    private val purple = layerColor(3)
    private val darkBlue = layerColor(4)

    @Test
    fun `up to 20 hearts keeps vanilla hearts`() {
        assertFalse(HealthBars.compact(40f, 0))
        assertTrue(HealthBars.compact(40f, 1))
        assertTrue(HealthBars.compact(41f, 0))
    }

    @Test
    fun `colors go orange, red, pink, purple, dark blue, cyan, green, yellowish green, then loop from red`() {
        assertEquals(0xFFFF8C1A.toInt(), orange)
        assertEquals(HealthBars.PALETTE.toList(), (1..7).map(::layerColor))
        assertEquals(red, layerColor(8))
        assertFalse(GOLD in (0..20).map(::layerColor))
    }

    @Test
    fun `the thin bar shows every layer, then gold, then missing`() {
        val bar = HealthBars.thin(health = 150.0, maxHealth = 200.0, absorption = 10.0)
        assertEquals(
            listOf(
                Segment(0.0, 40.0, orange),
                Segment(40.0, 80.0, red),
                Segment(80.0, 120.0, pink),
                Segment(120.0, 150.0, purple),
                Segment(150.0, 160.0, GOLD),
                Segment(160.0, 210.0, MISSING),
            ),
            bar,
        )
        assertEquals(listOf(40.0, 80.0, 120.0, 160.0, 200.0), HealthBars.layerEdges(200.0, 10.0))
    }

    @Test
    fun `the thick bar is the top layer over a full bar of the one below`() {
        // 150 health: layer 4 (purple) is 30 of 40 full, over a full pink bar.
        assertEquals(
            listOf(Segment(0.0, 30.0, purple), Segment(30.0, 40.0, pink)),
            HealthBars.thick(health = 150.0, maxHealth = 200.0, absorption = 0.0),
        )
    }

    @Test
    fun `a full layer shows full, not empty`() {
        assertEquals(listOf(Segment(0.0, 40.0, pink)), HealthBars.thick(120.0, 200.0, 0.0))
    }

    @Test
    fun `the first layer has missing health under it`() {
        assertEquals(listOf(Segment(0.0, 25.0, orange), Segment(25.0, 40.0, MISSING)), HealthBars.thick(25.0, 200.0, 0.0))
        assertEquals(listOf(Segment(0.0, 40.0, MISSING)), HealthBars.thick(0.0, 200.0, 0.0))
    }

    @Test
    fun `golden hearts are on top of health`() {
        // 150 health and 20 absorption reach 170: layer 5 holds 10 gold over layer 4 (purple, then gold).
        assertEquals(
            listOf(Segment(0.0, 10.0, GOLD), Segment(10.0, 30.0, purple), Segment(30.0, 40.0, GOLD)),
            HealthBars.thick(health = 150.0, maxHealth = 200.0, absorption = 20.0),
        )
    }

    @Test
    fun `layers past the palette loop back to red`() {
        val bar = HealthBars.thin(health = 1024.0, maxHealth = 1024.0, absorption = 0.0)
        assertEquals(26, bar.size)
        assertEquals(red, bar[8].color)
        assertEquals(darkBlue, bar[4].color)
    }
}
