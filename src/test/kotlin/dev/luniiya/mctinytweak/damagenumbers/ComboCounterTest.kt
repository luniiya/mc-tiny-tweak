package dev.luniiya.mctinytweak.damagenumbers

import dev.luniiya.mctinytweak.healthbar.HealthBars
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ComboCounterTest {
    @Test
    fun `hits in a row count up and add their damage`() {
        val combo = ComboCounter()
        assertNull(combo.sinceHit(0))
        combo.hit(100, 7f)
        combo.hit(113, 10.5f)
        combo.hit(126, 7f)
        assertEquals(3, combo.count)
        assertEquals(24.5f, combo.total, 1e-4f)
        assertTrue(combo.active(126 + ComboCounter.BREAK_TICKS - 1L))
    }

    @Test
    fun `the combo breaks after a pause and starts over`() {
        val combo = ComboCounter()
        combo.hit(0, 5f)
        combo.hit(10, 5f)
        assertFalse(combo.active(10 + ComboCounter.BREAK_TICKS.toLong()))
        combo.hit(10 + ComboCounter.BREAK_TICKS.toLong(), 3f)
        assertEquals(1, combo.count)
        assertEquals(3f, combo.total, 1e-4f)
        combo.clear()
        assertEquals(0, combo.count)
        assertFalse(combo.active(0))
    }

    @Test
    fun `it shows from two hits, stays solid, then slides out fading`() {
        assertFalse(ComboCounter.shown(1))
        assertTrue(ComboCounter.shown(2))
        assertEquals(1f, ComboCounter.alpha(ComboCounter.BREAK_TICKS.toFloat()), 1e-4f)
        assertEquals(0f, ComboCounter.alpha((ComboCounter.BREAK_TICKS + ComboCounter.FADE_TICKS).toFloat()), 1e-4f)
        assertEquals(0f, ComboCounter.slide(10f), 1e-4f)
        assertEquals(1f, ComboCounter.slide((ComboCounter.BREAK_TICKS + ComboCounter.FADE_TICKS).toFloat()), 1e-4f)
        assertEquals(1f, ComboCounter.remaining(0f), 1e-4f)
        assertEquals(0f, ComboCounter.remaining(ComboCounter.BREAK_TICKS.toFloat()), 1e-4f)
    }

    @Test
    fun `ranks climb with the combo and record when they are reached`() {
        assertNull(ComboCounter.rank(9))
        assertEquals("NICE", ComboCounter.rank(10))
        assertEquals("GREAT", ComboCounter.rank(25))
        assertEquals("AWESOME", ComboCounter.rank(30))
        assertEquals("INSANE", ComboCounter.rank(99))
        assertEquals("GODLIKE", ComboCounter.rank(100))
        assertEquals(5, ComboCounter.rankLevel(500))
        val combo = ComboCounter()
        repeat(9) { combo.hit(it.toLong(), 1f) }
        assertNull(combo.sinceRank(9))
        assertNull(combo.sinceMilestone(9))
        combo.hit(9, 1f)
        assertEquals(0f, combo.sinceRank(9)!!, 1e-4f)
        assertEquals(0f, combo.sinceMilestone(9)!!, 1e-4f)
        combo.hit(12, 1f)
        assertEquals(3f, combo.sinceRank(12)!!, 1e-4f, "11 hits is still NICE")
    }

    @Test
    fun `longer combos slam, shake and flash harder, milestones hardest`() {
        assertTrue(ComboCounter.pop(0f, 25) > ComboCounter.pop(0f, 3))
        assertTrue(ComboCounter.pop(0f, 20) > ComboCounter.pop(0f, 25))
        assertEquals(1f, ComboCounter.pop(ComboCounter.POP_TICKS.toFloat(), 20), 1e-4f)
        assertEquals(0f, ComboCounter.flash(0f, 5))
        assertEquals(1f, ComboCounter.flash(0f, 15), 1e-4f)
        assertEquals(0f, ComboCounter.shake(ComboCounter.POP_TICKS.toFloat(), 20, 0), 1e-4f)
        assertEquals(1f, ComboCounter.heartbeat(3f, 5), "short combos do not pulse")
        assertTrue((0 until 40).any { ComboCounter.heartbeat(it.toFloat(), 60) > 1.05f })
    }

    @Test
    fun `each hit echoes out, milestones burst shards and a shockwave`() {
        val (start, startAlpha) = ComboCounter.echo(0f)
        val (end, endAlpha) = ComboCounter.echo(ComboCounter.ECHO_TICKS.toFloat())
        assertEquals(1f, start, 1e-4f)
        assertTrue(end > 2f)
        assertTrue(startAlpha > 0f)
        assertEquals(0f, endAlpha, 1e-4f)
        val (r0, _) = ComboCounter.shockwave(0f)!!
        val (r1, _) = ComboCounter.shockwave(5f)!!
        assertTrue(r1 > r0)
        assertNull(ComboCounter.shockwave(ComboCounter.BURST_TICKS.toFloat()))
        val shards = (0 until ComboCounter.SHARDS).map { ComboCounter.shard(it, 6f)!! }
        assertTrue(shards.map { it.first }.distinct().size > ComboCounter.SHARDS / 2, "shards fly different ways")
        assertNull(ComboCounter.shard(0, ComboCounter.BURST_TICKS.toFloat()))
    }

    @Test
    fun `streaks from 50, rainbow and sparkles from 100, colors climb every 10`() {
        assertFalse(ComboCounter.streaks(49))
        assertTrue(ComboCounter.streaks(50))
        assertFalse(ComboCounter.rainbow(99))
        assertTrue(ComboCounter.rainbow(100))
        assertEquals(HealthBars.layerColor(0), ComboCounter.color(9))
        assertEquals(HealthBars.layerColor(2), ComboCounter.color(25))
        assertTrue((0 until 100).all { ComboCounter.streak(3, it.toFloat()) in 0f..1f })
        assertTrue((0 until 100).any { ComboCounter.twinkle(2, it.toFloat()) > 0.8f })
        assertTrue(ComboCounter.rankScale(0f) > 2f)
        assertEquals(1f, ComboCounter.rankScale(ComboCounter.RANK_TICKS.toFloat()), 0.06f)
    }

    @Test
    fun `combo popups burst out from the mob and settle on their spot`() {
        assertEquals(ComboCounter.SPREAD_FROM, ComboCounter.spread(0f, 0), 1e-4f)
        // Overshooting past their spot on the way out, then resting on it.
        assertTrue(ComboCounter.spread(ComboCounter.POP_TICKS * 0.6f, 0) > 1f)
        assertEquals(1f, ComboCounter.spread(ComboCounter.POP_TICKS.toFloat() + 2f, 0), 1e-4f)
        // Later popups of the same hit leave a moment after.
        assertTrue(ComboCounter.spread(1f, 2) < ComboCounter.spread(1f, 0))
    }

    @Test
    fun `count popups spawn on either side of the mob, never above or below it`() {
        val spots = (0 until 200).map { ComboCounter.around(ComboCounter.sideAngle(it / 200f)) }
        assertTrue(spots.any { it.first > 0.5f } && spots.any { it.first < -0.5f })
        // Above is where the damage number rises, so they keep clear of straight up and down.
        val maxUp = kotlin.math.sin(ComboCounter.SIDE_SPREAD) + 1e-4f
        assertTrue(spots.all { kotlin.math.abs(it.second) <= maxUp })
        assertTrue(spots.all { kotlin.math.abs(it.first) >= kotlin.math.cos(ComboCounter.SIDE_SPREAD) - 1e-4f })
    }

    @Test
    fun `the total's underline shoots out once the total lands, then stays full`() {
        assertEquals(0f, ComboCounter.underline(0f), 1e-4f)
        assertEquals(0f, ComboCounter.underline(ComboCounter.UNDERLINE_DELAY), 1e-4f)
        val mid = ComboCounter.underline(ComboCounter.UNDERLINE_DELAY + ComboCounter.UNDERLINE_TICKS / 2)
        assertTrue(mid > 0.5f && mid < 1f)
        assertEquals(1f, ComboCounter.underline(20f), 1e-4f)
    }
}
