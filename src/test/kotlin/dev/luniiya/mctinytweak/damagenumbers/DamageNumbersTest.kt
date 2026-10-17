package dev.luniiya.mctinytweak.damagenumbers

import dev.luniiya.mctinytweak.healthbar.HealthBars
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
        val crits = DamageNumbers.Marks()
        crits.mark(7, 100)
        assertFalse(crits.take(8, 100))
        assertTrue(crits.take(7, 101))
        assertFalse(crits.take(7, 101))
        crits.mark(7, 100)
        assertFalse(crits.take(7, 100 + DamageNumbers.MARK_WINDOW + 1L))
    }

    @Test
    fun `old crits are pruned`() {
        val crits = DamageNumbers.Marks()
        crits.mark(1, 10)
        crits.prune(10 + DamageNumbers.MARK_WINDOW + 1L)
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
    fun `hits climb the heart colors, one color per doubling`() {
        assertEquals(0, DamageNumbers.tier(4.9f))
        assertEquals(1, DamageNumbers.tier(5f))
        assertEquals(1, DamageNumbers.tier(9.9f))
        assertEquals(2, DamageNumbers.tier(10f))
        assertEquals(4, DamageNumbers.tier(40f))
        assertEquals(HealthBars.layerColor(0), DamageNumbers.color(2f))
        assertEquals(HealthBars.layerColor(3), DamageNumbers.color(25f))
        // Past the last color the palette loops back to red, like the health bar.
        assertEquals(HealthBars.layerColor(1), DamageNumbers.color(5f * 128))
    }

    @Test
    fun `crits flip the brightness, a dark fill with a bright outline`() {
        val color = DamageNumbers.color(25f)
        fun brightness(c: Int) = ((c shr 16) and 0xFF) + ((c shr 8) and 0xFF) + (c and 0xFF)
        assertEquals(color, DamageNumbers.fill(color, crit = false))
        assertTrue(brightness(DamageNumbers.outline(color, crit = false)) < brightness(color))
        assertTrue(brightness(DamageNumbers.fill(color, crit = true)) < brightness(color))
        assertTrue(brightness(DamageNumbers.outline(color, crit = true)) > brightness(color))
        assertTrue(brightness(DamageNumbers.outline(color, crit = true)) > brightness(DamageNumbers.fill(color, crit = true)))
    }

    @Test
    fun `intensity grows with damage, crits hit harder, and it decides big and huge`() {
        assertEquals(0f, DamageNumbers.intensity(4f, false), 1e-4f)
        assertEquals(1f, DamageNumbers.intensity(32f, false), 1e-4f)
        assertEquals(1f, DamageNumbers.intensity(5000f, false), 1e-4f)
        assertTrue(DamageNumbers.intensity(6f, true) > DamageNumbers.intensity(6f, false))
        assertFalse(DamageNumbers.isBig(DamageNumbers.intensity(6f, false), false))
        assertTrue(DamageNumbers.isBig(DamageNumbers.intensity(12f, false), false))
        assertTrue(DamageNumbers.isBig(0f, true))
        assertFalse(DamageNumbers.isHuge(DamageNumbers.intensity(12f, false), false))
        assertTrue(DamageNumbers.isHuge(DamageNumbers.intensity(30f, false), false))
    }

    @Test
    fun `slam starts big, bounces past its size and settles`() {
        val hard = 1f
        val peak = DamageNumbers.scale(0f, hard, slam = true)
        val settled = DamageNumbers.scale(DamageNumbers.SLAM_TICKS.toFloat(), hard, slam = true)
        assertTrue(peak > 3f * settled / 1.6f)
        val dip = (1..DamageNumbers.SLAM_TICKS * 4).minOf { DamageNumbers.scale(it / 4f, hard, slam = true) }
        assertTrue(dip < settled)
        assertEquals(settled, DamageNumbers.scale(20f, hard, slam = true), 1e-4f)
        // Harder hits stay bigger, and without slam they only pop a little.
        assertTrue(settled > DamageNumbers.scale(20f, 0f, slam = true))
        assertTrue(DamageNumbers.scale(0f, hard, slam = false) < peak)
    }

    @Test
    fun `numbers flash, shake, count up, rise and fade out`() {
        assertEquals(1f, DamageNumbers.flash(0f), 1e-4f)
        assertEquals(0f, DamageNumbers.flash(DamageNumbers.FLASH_TICKS.toFloat()), 1e-4f)
        assertEquals(0f, DamageNumbers.shakeX(DamageNumbers.SHAKE_TICKS.toFloat(), 1f, 3f), 1e-4f)
        assertTrue((1..6).any { kotlin.math.abs(DamageNumbers.shakeX(it / 2f, 1f, 3f)) > 1f })
        assertEquals(0f, DamageNumbers.countUp(50f, 0f), 1e-4f)
        assertTrue(DamageNumbers.countUp(50f, 2f) in 1f..50f)
        assertEquals(50f, DamageNumbers.countUp(50f, DamageNumbers.COUNT_TICKS.toFloat()), 1e-4f)
        assertEquals(1f, DamageNumbers.alpha(0f), 1e-4f)
        assertEquals(0f, DamageNumbers.alpha(DamageNumbers.LIFETIME.toFloat()), 1e-4f)
        assertEquals(0f, DamageNumbers.rise(0f), 1e-4f)
        assertTrue(DamageNumbers.rise(12f) < DamageNumbers.rise(24f))
        // Longer lifetimes fade and rise later.
        assertEquals(1f, DamageNumbers.alpha(DamageNumbers.LIFETIME.toFloat(), lifetime = 40), 1e-4f)
        assertTrue(DamageNumbers.rise(12f, lifetime = 40) < DamageNumbers.rise(12f))
        assertEquals(0x80FF0000.toInt(), DamageNumbers.withAlpha(0xFFFF0000.toInt(), 128 / 255f))
    }

    @Test
    fun `gradient flows between the hit's color, the next heart color and gold`() {
        val own = DamageNumbers.color(30f)
        assertEquals(own, DamageNumbers.gradientColor(30f, 0, 0f))
        assertNotEquals(DamageNumbers.gradientColor(30f, 0, 0f), DamageNumbers.gradientColor(30f, 2, 0f))
        assertNotEquals(DamageNumbers.gradientColor(30f, 0, 0f), DamageNumbers.gradientColor(30f, 0, 5f))
        assertEquals(0xFF808080.toInt(), DamageNumbers.mix(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0.5f))
    }

    @Test
    fun `numbers are pulled toward the camera to stay in front of flames`() {
        // 10 blocks away it is drawn 1.5 blocks closer.
        assertEquals(8.5, 10 * DamageNumbers.pull(10.0), 1e-9)
        // Close up it only moves halfway, never past the camera.
        assertEquals(0.5, DamageNumbers.pull(1.0), 1e-9)
        assertEquals(1.0, DamageNumbers.pull(0.0), 1e-9)
    }

    @Test
    fun `fat mobs pull numbers and effects further, so their body cannot cover them`() {
        // A zombie is thin, so the usual pull is enough.
        assertEquals(DamageNumbers.PULL, DamageNumbers.clearance(0.6), 1e-9)
        // A sniffer is 1.9 wide: from anywhere over its body, the pull clears its near side.
        val sniffer = DamageNumbers.clearance(1.9)
        assertTrue(sniffer > 1.9 * Math.sqrt(2.0) / 2 + 1.0)
        assertEquals(10 - sniffer, 10 * DamageNumbers.pull(10.0, sniffer), 1e-9)
        // Up close it still stops halfway to the camera.
        assertEquals(0.5, DamageNumbers.pull(3.0, sniffer), 1e-9)
    }

    @Test
    fun `crit marks and combos`() {
        assertEquals("7✦", DamageNumbers.label(7f, crit = true, critMark = true))
        assertEquals("7", DamageNumbers.label(7f, crit = true, critMark = false))
        assertEquals("7", DamageNumbers.label(7f, crit = false, critMark = true))
        assertTrue(DamageNumbers.continuesCombo(DamageNumbers.COMBO_TICKS))
        assertFalse(DamageNumbers.continuesCombo(DamageNumbers.COMBO_TICKS + 1))
    }

    @Test
    fun `kill shows a glitching NULL that stays four letters and sometimes is clean`() {
        val texts = (0 until 200).map { DamageNumbers.glitchText(it, 42) }
        assertTrue(texts.all { it.length == 4 })
        assertTrue(DamageNumbers.NULL_TEXT in texts)
        assertTrue(texts.any { it != DamageNumbers.NULL_TEXT })
        assertEquals(DamageNumbers.glitchText(5, 42), DamageNumbers.glitchText(5, 42))
        assertTrue((0 until 200).all { DamageNumbers.glitchSplit(it, 1) in 1f..4f })
        assertTrue((0 until 200).all { DamageNumbers.glitchJolt(it, 1) in -4f..4f })
        assertTrue((0 until 200).any { DamageNumbers.glitchJolt(it, 1) != 0f })
    }

    @Test
    fun `the last attacker counts through the invulnerability window, then is forgotten`() {
        val attackers = DamageNumbers.Attackers()
        assertNull(attackers.of(1, 100))
        attackers.record(1, 42, 100)
        assertEquals(42, attackers.of(1, 100))
        // A harder hit 10 ticks later comes without a damage event, but is still the same attacker's.
        assertEquals(42, attackers.of(1, 110))
        assertNull(attackers.of(1, 100 + DamageNumbers.HURT_TICKS + 1L))
        // Damage from no one, like fire or /damage without a source, has no attacker.
        attackers.record(2, -1, 100)
        assertNull(attackers.of(2, 100))
        attackers.prune(100 + DamageNumbers.HURT_TICKS + 1L)
        attackers.record(3, 7, 200)
        assertNull(attackers.of(1, 100))
        assertEquals(7, attackers.of(3, 200))
    }

    @Test
    fun `only my hits hides everyone else's, off shows all`() {
        assertTrue(DamageNumbers.shows(onlyMine = false, attacker = null, me = 5))
        assertTrue(DamageNumbers.shows(onlyMine = false, attacker = 9, me = 5))
        assertTrue(DamageNumbers.shows(onlyMine = true, attacker = 5, me = 5))
        assertFalse(DamageNumbers.shows(onlyMine = true, attacker = 9, me = 5))
        assertFalse(DamageNumbers.shows(onlyMine = true, attacker = null, me = 5))
    }

    @Test
    fun `spots in front of the camera are in view, behind it are not`() {
        val cos = DamageNumbers.viewCos(70.0, 16.0 / 9)
        assertTrue(DamageNumbers.inView(0.0, 0.0, 10.0, 0.0, 0.0, 1.0, cos))
        assertFalse(DamageNumbers.inView(0.0, 0.0, -10.0, 0.0, 0.0, 1.0, cos))
        assertFalse(DamageNumbers.inView(10.0, 0.0, 0.0, 0.0, 0.0, 1.0, cos), "straight to the side is off screen")
        // 50 degrees to the side is on a wide screen but not a narrow one.
        val side = Math.toRadians(50.0)
        assertTrue(DamageNumbers.inView(Math.sin(side), 0.0, Math.cos(side), 0.0, 0.0, 1.0, cos))
        assertFalse(DamageNumbers.inView(Math.sin(side), 0.0, Math.cos(side), 0.0, 0.0, 1.0, DamageNumbers.viewCos(70.0, 1.0)))
        assertTrue(DamageNumbers.inView(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, cos), "right at the camera counts")
    }
}
