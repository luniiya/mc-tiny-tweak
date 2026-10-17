package dev.luniiya.mctinytweak.damagenumbers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DamageNumberStyleTest {
    @Test
    fun `every effect is on by default`() {
        val style = DamageNumberStyle()
        assertTrue(listOf(style.slam, style.flash, style.shake, style.burst, style.gradient, style.combo, style.countUp, style.critMark).all { it })
        assertTrue(listOf(style.font, style.rings, style.slash, style.afterimages, style.rays, style.shatter, style.focusLines, style.rainbow, style.impactFrame, style.sounds).all { it })
        assertFalse(style.onlyMine, "everyone's hits show by default")
        assertTrue(style.healNumbers && style.comboCounter)
        assertEquals(style, DamageNumberStyle.parse(""))
    }

    @Test
    fun `each effect survives saving and loading on its own`() {
        val styles = listOf(
            DamageNumberStyle(slam = false),
            DamageNumberStyle(flash = false),
            DamageNumberStyle(shake = false),
            DamageNumberStyle(burst = false),
            DamageNumberStyle(gradient = false),
            DamageNumberStyle(combo = false),
            DamageNumberStyle(countUp = false),
            DamageNumberStyle(critMark = false),
            DamageNumberStyle(font = false),
            DamageNumberStyle(rings = false),
            DamageNumberStyle(slash = false),
            DamageNumberStyle(afterimages = false),
            DamageNumberStyle(rays = false),
            DamageNumberStyle(shatter = false),
            DamageNumberStyle(focusLines = false),
            DamageNumberStyle(rainbow = false),
            DamageNumberStyle(impactFrame = false),
            DamageNumberStyle(sounds = false),
            DamageNumberStyle(onlyMine = true),
            DamageNumberStyle(healNumbers = false),
            DamageNumberStyle(comboCounter = false),
        )
        for (style in styles) assertEquals(style, DamageNumberStyle.parse(style.serialize()))
    }

    @Test
    fun `bad lines and values fall back to defaults`() {
        assertEquals(DamageNumberStyle(), DamageNumberStyle.parse("# note\nslam = maybe\nnope\nother=false\n"))
        assertFalse(DamageNumberStyle.parse("  combo = false ").combo)
    }
}
