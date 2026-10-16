package dev.luniiya.mctinytweak.damagenumbers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DamageNumbersTest {
    @Test
    fun `a new entity gives no number, a health drop gives the amount lost`() {
        val tracker = DamageNumbers.HealthTracker()
        assertNull(tracker.update(1, 20f))
        assertNull(tracker.update(1, 20f))
        assertEquals(6f, tracker.update(1, 14f)!!, 1e-4f)
        assertNull(tracker.update(1, 14f))
    }

    @Test
    fun `healing and rounding noise give no number`() {
        val tracker = DamageNumbers.HealthTracker()
        tracker.update(1, 10f)
        assertNull(tracker.update(1, 12f))
        assertNull(tracker.update(1, 11.99f))
        assertEquals(2f, tracker.update(1, 9.99f)!!, 1e-4f)
    }

    @Test
    fun `entities are tracked separately and forgotten when gone`() {
        val tracker = DamageNumbers.HealthTracker()
        tracker.update(1, 20f)
        tracker.update(2, 8f)
        assertEquals(3f, tracker.update(2, 5f)!!, 1e-4f)
        tracker.retain(setOf(2))
        assertEquals(1, tracker.size)
        // A reused id starts fresh instead of comparing with the old entity's health.
        assertNull(tracker.update(1, 4f))
    }

    @Test
    fun `a crit counts only right after its animation and only once`() {
        val crits = DamageNumbers.Crits()
        crits.mark(7, 100)
        assertFalse(crits.take(8, 100))
        assertTrue(crits.take(7, 101))
        assertFalse(crits.take(7, 101))
        crits.mark(7, 100)
        assertFalse(crits.take(7, 100 + DamageNumbers.CRIT_WINDOW + 1L))
    }

    @Test
    fun `old crits are pruned`() {
        val crits = DamageNumbers.Crits()
        crits.mark(1, 10)
        crits.prune(10 + DamageNumbers.CRIT_WINDOW + 1L)
        assertFalse(crits.take(1, 10))
    }

    @Test
    fun `numbers show one decimal only when it matters`() {
        assertEquals("6", DamageNumbers.format(6f))
        assertEquals("4.5", DamageNumbers.format(4.5f))
        assertEquals("0.3", DamageNumbers.format(0.3f))
        assertEquals("5", DamageNumbers.format(4.98f))
        assertEquals("12", DamageNumbers.format(12.4f))
        assertEquals("13", DamageNumbers.format(12.6f))
        assertEquals("1024", DamageNumbers.format(1024f))
    }

    @Test
    fun `bigger hits get warmer colors and crits are gold`() {
        assertEquals(DamageNumbers.WHITE, DamageNumbers.color(2f, false))
        assertEquals(DamageNumbers.YELLOW, DamageNumbers.color(4f, false))
        assertEquals(DamageNumbers.ORANGE, DamageNumbers.color(10f, false))
        assertEquals(DamageNumbers.RED, DamageNumbers.color(50f, false))
        assertEquals(DamageNumbers.CRIT, DamageNumbers.color(2f, true))
    }

    @Test
    fun `numbers pop in, settle, rise and fade out`() {
        assertTrue(DamageNumbers.scale(0f, false) > 1.5f)
        assertEquals(1f, DamageNumbers.scale(DamageNumbers.POP_TICKS.toFloat(), false), 1e-4f)
        assertTrue(DamageNumbers.scale(10f, true) > DamageNumbers.scale(10f, false))
        assertEquals(1f, DamageNumbers.alpha(0f), 1e-4f)
        assertEquals(0f, DamageNumbers.alpha(DamageNumbers.LIFETIME.toFloat()), 1e-4f)
        assertEquals(0f, DamageNumbers.rise(0f), 1e-4f)
        assertTrue(DamageNumbers.rise(12f) < DamageNumbers.rise(24f))
        assertEquals(0x80FF0000.toInt(), DamageNumbers.withAlpha(0xFFFF0000.toInt(), 128 / 255f))
    }
}
