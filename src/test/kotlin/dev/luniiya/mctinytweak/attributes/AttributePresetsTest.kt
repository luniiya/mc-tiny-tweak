package dev.luniiya.mctinytweak.attributes

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AttributePresetsTest {
    private val defaults = mapOf("minecraft:scale" to 1.0, "minecraft:gravity" to 0.08, "minecraft:max_health" to 20.0)

    @Test
    fun `default is always listed first and cannot be saved over`() {
        val presets = AttributePresets().save("tiny", defaults)
        assertEquals(listOf("Default", "tiny"), presets.names)
        assertThrows<IllegalArgumentException> { presets.save("default", defaults) }
        assertThrows<IllegalArgumentException> { presets.save("  ", defaults) }
        assertFalse(AttributePresets.isValidName(" Default "))
    }

    @Test
    fun `saving a name that differs only in case replaces it`() {
        val presets = AttributePresets().save("Tiny", mapOf("minecraft:scale" to 0.5)).save("tiny", mapOf("minecraft:scale" to 0.25))
        assertEquals(mapOf("tiny" to mapOf("minecraft:scale" to 0.25)), presets.presets)
    }

    @Test
    fun `loading default resets only attributes that changed`() {
        val current = defaults + ("minecraft:scale" to 3.0)
        assertEquals(listOf("attribute @s minecraft:scale base reset"), AttributePresets().loadCommands("Default", current, defaults))
    }

    @Test
    fun `loading a preset sets its values and resets attributes it does not mention`() {
        val presets = AttributePresets().save("floaty", mapOf("minecraft:gravity" to 0.01, "minecraft:max_health" to 20.0))
        val current = defaults + ("minecraft:scale" to 2.0)
        assertEquals(
            listOf("attribute @s minecraft:gravity base set 0.01", "attribute @s minecraft:scale base reset"),
            presets.loadCommands("floaty", current, defaults),
        )
    }

    @Test
    fun `unknown presets load nothing`() {
        assertEquals(emptyList<String>(), AttributePresets().loadCommands("missing", defaults, defaults))
    }

    @Test
    fun `presets survive saving and loading`() {
        val presets = AttributePresets().save("giant", mapOf("minecraft:scale" to 16.0, "minecraft:waypoint_transmit_range" to 6.0E7))
        assertEquals(presets, AttributePresets.parse(presets.serialize()))
    }

    @Test
    fun `bad entries in the file are skipped`() {
        val text = """{"Default": {"minecraft:scale": 2}, "ok": {"minecraft:scale": 2, "minecraft:luck": "lots"}, "list": [1]}"""
        assertEquals(mapOf("ok" to mapOf("minecraft:scale" to 2.0)), AttributePresets.parse(text).presets)
        assertEquals(AttributePresets(), AttributePresets.parse("[]"))
    }

    @Test
    fun `deleting removes the preset`() {
        assertEquals(listOf("Default"), AttributePresets().save("a", defaults).delete("a").names)
    }
}
