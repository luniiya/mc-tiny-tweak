package dev.luniiya.mctinytweak.settings

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TweakSettingsTest {
    @Test
    fun `armor is hidden by default`() {
        assertFalse(TweakSettings().showArmor)
        assertFalse(TweakSettings.parse("").showArmor)
    }

    @Test
    fun `fullbright is off by default`() {
        assertFalse(TweakSettings().fullbright)
        assertFalse(TweakSettings.parse("showArmor=true").fullbright)
    }

    @Test
    fun `settings survive saving and loading`() {
        val shown = TweakSettings(showArmor = true)
        assertEquals(shown, TweakSettings.parse(shown.serialize()))
        val bright = TweakSettings(fullbright = true)
        assertEquals(bright, TweakSettings.parse(bright.serialize()))
    }

    @Test
    fun `comments, unknown keys and bad values fall back to defaults`() {
        val parsed = TweakSettings.parse("# saved by the mod\nsomethingElse=1\nshowArmor = maybe\nno separator\n")
        assertEquals(TweakSettings(), parsed)
        assertTrue(TweakSettings.parse("  showArmor = true  ").showArmor)
    }
}
