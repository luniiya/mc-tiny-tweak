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
        assertFalse(presets.canSave(" Default "))
    }

    @Test
    fun `built-in presets come after default, load, and cannot be saved over or stored`() {
        val builtIn = mapOf("god" to mapOf("minecraft:max_health" to 1024.0))
        val presets = AttributePresets(builtIn = builtIn).save("mine", defaults)
        assertEquals(listOf("Default", "god", "mine"), presets.names)
        assertFalse(presets.canSave("GOD"))
        assertThrows<IllegalArgumentException> { presets.save("god", defaults) }
        assertEquals(
            listOf("attribute @s minecraft:max_health base set 1024"),
            presets.loadCommands("god", defaults, defaults),
        )
        assertEquals(setOf("mine"), AttributePresets.parse(presets.serialize()).presets.keys)
    }

    @Test
    fun `a saved preset with a built-in name is hidden behind the built-in one`() {
        val builtIn = mapOf("god" to mapOf("minecraft:max_health" to 1024.0))
        val presets = AttributePresets.parse("""{"god": {"minecraft:max_health": 5}}""", builtIn)
        assertEquals(listOf("Default", "god"), presets.names)
        assertEquals(mapOf("minecraft:max_health" to 1024.0), presets.values("god"))
    }

    @Test
    fun `the bundled presets are the user's boosted, default smol and god`() {
        val bundled = AttributePresets.bundled()
        assertEquals(setOf("boosted", "default smol", "god"), bundled.keys)
        assertEquals(0.85, bundled.getValue("default smol")["minecraft:scale"])
        assertEquals(1024.0, bundled.getValue("god")["minecraft:max_health"])
        assertEquals(160.0, bundled.getValue("boosted")["minecraft:max_health"])
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
