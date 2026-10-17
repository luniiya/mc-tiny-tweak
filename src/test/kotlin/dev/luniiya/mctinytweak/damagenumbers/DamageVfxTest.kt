package dev.luniiya.mctinytweak.damagenumbers

import dev.luniiya.mctinytweak.healthbar.HealthBars
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.abs

class DamageVfxTest {
    @Test
    fun `tiers start at 100, 200, 500 and 990`() {
        assertEquals(DamageVfx.NONE, DamageVfx.tier(99.9f))
        assertEquals(DamageVfx.HEAVY, DamageVfx.tier(100f))
        assertEquals(DamageVfx.HEAVY, DamageVfx.tier(199f))
        assertEquals(DamageVfx.BRUTAL, DamageVfx.tier(200f))
        assertEquals(DamageVfx.BRUTAL, DamageVfx.tier(499f))
        assertEquals(DamageVfx.DEVASTATING, DamageVfx.tier(500f))
        assertEquals(DamageVfx.DEVASTATING, DamageVfx.tier(989f))
        assertEquals(DamageVfx.MAX, DamageVfx.tier(990f))
        assertEquals(DamageVfx.MAX, DamageVfx.tier(1024f))
    }

    @Test
    fun `bigger tiers stay longer and pile on more of everything`() {
        assertEquals(DamageNumbers.LIFETIME, DamageVfx.lifetime(DamageVfx.NONE))
        for (tier in DamageVfx.NONE until DamageVfx.MAX) {
            assertTrue(DamageVfx.lifetime(tier + 1) > DamageVfx.lifetime(tier))
            assertTrue(DamageVfx.rings(tier + 1) > DamageVfx.rings(tier))
            assertTrue(DamageVfx.slashes(tier + 1) > DamageVfx.slashes(tier))
            assertTrue(DamageVfx.afterimages(tier + 1) >= DamageVfx.afterimages(tier))
            assertTrue(DamageVfx.rays(tier + 1) >= DamageVfx.rays(tier))
        }
        // Each effect starts at its own tier.
        assertEquals(0, DamageVfx.rings(DamageVfx.NONE))
        assertEquals(0, DamageVfx.afterimages(DamageVfx.HEAVY))
        assertTrue(DamageVfx.afterimages(DamageVfx.BRUTAL) > 0)
        assertEquals(0, DamageVfx.rays(DamageVfx.BRUTAL))
        assertTrue(DamageVfx.rays(DamageVfx.DEVASTATING) > 0)
        assertEquals(0, DamageVfx.focusLines(DamageVfx.BRUTAL))
        assertTrue(DamageVfx.focusLines(DamageVfx.MAX) > DamageVfx.focusLines(DamageVfx.DEVASTATING))
    }

    @Test
    fun `rings expand one after another, thin out and fade`() {
        assertEquals(0f, DamageVfx.ringRadius(0f, 0, DamageVfx.HEAVY)!!, 1e-4f)
        assertNull(DamageVfx.ringRadius(0f, 1, DamageVfx.BRUTAL), "the second ring waits its turn")
        val early = DamageVfx.ringRadius(2f, 0, DamageVfx.HEAVY)!!
        val late = DamageVfx.ringRadius(7f, 0, DamageVfx.HEAVY)!!
        assertTrue(late > early)
        assertTrue(DamageVfx.ringWidth(7f, 0) < DamageVfx.ringWidth(1f, 0))
        assertTrue(DamageVfx.ringAlpha(7f, 0) < DamageVfx.ringAlpha(1f, 0))
        assertNull(DamageVfx.ringRadius(DamageVfx.RING_TICKS.toFloat(), 0, DamageVfx.HEAVY))
        assertEquals(0f, DamageVfx.ringAlpha(DamageVfx.RING_TICKS.toFloat(), 0))
    }

    @Test
    fun `slashes cut across, then thin out, each a moment after the last`() {
        assertEquals(0f, DamageVfx.slashLength(0f, 0), 1e-4f)
        assertEquals(1f, DamageVfx.slashLength(DamageVfx.SLASH_IN.toFloat(), 0), 1e-4f)
        assertEquals(1f, DamageVfx.slashWidth(1f, 0), 1e-4f)
        assertEquals(0f, DamageVfx.slashWidth((DamageVfx.SLASH_IN + DamageVfx.SLASH_OUT).toFloat(), 0), 1e-4f)
        assertEquals(0f, DamageVfx.slashWidth(0.5f, 1), "the second slash has not started")
        // An X: two slashes a quarter turn apart.
        val a = DamageVfx.slashAngle(0, 2, 7)
        val b = DamageVfx.slashAngle(1, 2, 7)
        assertEquals(Math.PI / 2, (b - a).toDouble(), 1e-5)
    }

    @Test
    fun `afterimages fade from the closest one and die out as the number settles`() {
        val count = DamageVfx.afterimages(DamageVfx.MAX)
        assertTrue(DamageVfx.afterimageAlpha(1f, 1, count) > DamageVfx.afterimageAlpha(1f, count, count))
        assertEquals(0f, DamageVfx.afterimageAlpha(DamageVfx.AFTERIMAGE_TICKS.toFloat(), 1, count), 1e-4f)
        assertEquals(0f, DamageVfx.afterimageAlpha(1f, count + 1, count))
    }

    @Test
    fun `rays spread evenly, spin, grow in and the MAX layers turn opposite ways`() {
        val count = DamageVfx.rays(DamageVfx.DEVASTATING)
        val step = DamageVfx.rayAngle(1, count, 0f) - DamageVfx.rayAngle(0, count, 0f)
        assertEquals(2 * Math.PI / count, step.toDouble(), 1e-5)
        assertTrue(DamageVfx.rayAngle(0, count, 10f) > DamageVfx.rayAngle(0, count, 0f))
        assertTrue(DamageVfx.rayAngle(0, count, 10f, layer = 1) < DamageVfx.rayAngle(0, count, 0f, layer = 1))
        assertEquals(0f, DamageVfx.rayLength(0f, DamageVfx.DEVASTATING), 1e-4f)
        assertTrue(DamageVfx.rayLength(5f, DamageVfx.MAX) > DamageVfx.rayLength(5f, DamageVfx.DEVASTATING))
    }

    @Test
    fun `numbers shatter in their last ticks, pieces flying out then falling`() {
        val lifetime = DamageVfx.lifetime(DamageVfx.DEVASTATING)
        assertEquals(0f, DamageVfx.shatter((lifetime - DamageVfx.SHATTER_TICKS).toFloat(), lifetime), 1e-4f)
        assertEquals(1f, DamageVfx.shatter(lifetime.toFloat(), lifetime), 1e-4f)
        val start = DamageVfx.shard(0, 0f, 3)
        assertEquals(0f, start.first, 1e-4f)
        assertEquals(0f, start.second, 1e-4f)
        // Pieces go different ways, and all end up falling.
        val ends = (0 until 6).map { DamageVfx.shard(it, 1f, 3) }
        assertTrue(ends.map { it.first }.distinct().size > 1)
        assertTrue(ends.all { DamageVfx.shard(ends.indexOf(it), 1f, 3).second > DamageVfx.shard(ends.indexOf(it), 0.3f, 3).second - 30f })
        assertEquals(DamageVfx.shard(2, 0.5f, 9), DamageVfx.shard(2, 0.5f, 9))
    }

    @Test
    fun `MAX cycles through every heart color`() {
        assertEquals(HealthBars.PALETTE.size + 2, DamageVfx.RAINBOW.size)
        assertEquals(HealthBars.GOLD, DamageVfx.RAINBOW.last())
        for (i in DamageVfx.RAINBOW.indices) assertEquals(DamageVfx.RAINBOW[i], DamageVfx.rainbow(0, i / 0.45f), "step $i")
        assertNotEquals(DamageVfx.rainbow(0, 0f), DamageVfx.rainbow(1, 0f))
    }

    @Test
    fun `the MAX tag pops in late and big, the starburst flares and collapses`() {
        assertEquals(0f, DamageVfx.maxTagScale(DamageVfx.MAX_TAG_DELAY.toFloat()))
        assertTrue(DamageVfx.maxTagScale(DamageVfx.MAX_TAG_DELAY + 0.1f) > 2f)
        assertEquals(1f, DamageVfx.maxTagScale(DamageVfx.MAX_TAG_DELAY + 5f), 0.1f)
        assertEquals(0f, DamageVfx.starScale(0f), 1e-4f)
        assertEquals(1f, DamageVfx.starScale(2f), 1e-4f)
        assertEquals(0f, DamageVfx.starScale(DamageVfx.STAR_TICKS.toFloat()))
    }

    @Test
    fun `the impact frame inverts the world first, then flashes white and clears`() {
        assertTrue(DamageVfx.impactInverted(0f))
        assertTrue(DamageVfx.impactInverted(1.9f))
        assertFalse(DamageVfx.impactInverted(DamageVfx.IMPACT_INVERT))
        assertFalse(DamageVfx.impactInverted(-1f))
        assertEquals(0f, DamageVfx.impactFlash(1f), "no flash while inverted")
        assertTrue(DamageVfx.impactFlash(DamageVfx.IMPACT_INVERT) > 0.5f)
        assertEquals(0f, DamageVfx.impactFlash(DamageVfx.IMPACT_INVERT + DamageVfx.IMPACT_FLASH))
    }

    @Test
    fun `focus lines snap in, fade, and flicker each tick`() {
        assertEquals(1f, DamageVfx.focusAlpha(0f), 1e-4f)
        assertEquals(0f, DamageVfx.focusAlpha(DamageVfx.FOCUS_TICKS.toFloat()), 1e-4f)
        val count = DamageVfx.focusLines(DamageVfx.MAX)
        val (angle, depth) = DamageVfx.focusLine(5, count, 0, 1)
        assertTrue(depth in 0.25f..0.6f)
        assertTrue(abs(angle - 5 * 2 * Math.PI.toFloat() / count) < 2 * Math.PI.toFloat() / count)
        assertNotEquals(DamageVfx.focusLine(5, count, 0, 1), DamageVfx.focusLine(5, count, 1, 1))
    }
}
