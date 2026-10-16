package dev.luniiya.mctinytweak.attributes

import dev.luniiya.mctinytweak.attributes.AttributeAutoApply.Decision
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AttributeAutoApplyTest {
    private val defaults = mapOf("minecraft:scale" to 1.0, "minecraft:max_health" to 20.0)
    private val presets = AttributePresets(
        presets = mapOf("tall" to mapOf("minecraft:scale" to 2.0)),
        builtIn = mapOf(
            "boosted" to mapOf("minecraft:scale" to 0.85, "minecraft:max_health" to 40.0),
            // Only differs in an attribute the server does not send, so it looks like defaults.
            "hits hard" to mapOf("minecraft:attack_damage" to 10.0),
        ),
    )
    private val auto = AttributeAutoApply()
    private val boosted = mapOf("minecraft:scale" to 0.85, "minecraft:max_health" to 40.0)

    @Test
    fun `boosted is applied by default`() {
        assertEquals("boosted", auto.preset)
        assertTrue("boosted" in AttributePresets.bundled())
    }

    @Test
    fun `a fresh player in a new world gets the auto preset`() {
        // "hits hard" also looks like these values, but all defaults means fresh, not a choice.
        assertEquals(Decision.Apply("boosted"), auto.decide("w", defaults, defaults, presets))
    }

    @Test
    fun `it is never applied twice in the same world`() {
        val applied = auto.remember("w", "boosted")
        assertEquals(Decision.Nothing, applied.decide("w", boosted, defaults, presets))
        // Even if the values look fresh again, for example after loading Default on purpose.
        assertEquals(Decision.Nothing, applied.remember("w", AttributePresets.DEFAULT).decide("w", defaults, defaults, presets))
        assertEquals(Decision.Apply("boosted"), applied.decide("other world", defaults, defaults, presets))
    }

    @Test
    fun `values that already match a preset are only remembered`() {
        assertEquals(Decision.Remember("tall"), auto.decide("w", defaults + ("minecraft:scale" to 2.0), defaults, presets))
        assertEquals(Decision.Remember("boosted"), auto.decide("w", boosted, defaults, presets))
    }

    @Test
    fun `values that match no preset get the auto preset`() {
        assertEquals(Decision.Apply("boosted"), auto.decide("w", defaults + ("minecraft:scale" to 3.0), defaults, presets))
    }

    @Test
    fun `nothing happens when auto apply is off or its preset is gone`() {
        assertEquals(Decision.Nothing, auto.withPreset(null).decide("w", defaults, defaults, presets))
        assertEquals(Decision.Nothing, auto.withPreset("tall").deleted("tall").decide("w", defaults, defaults, presets))
        assertEquals(Decision.Nothing, auto.withPreset("missing").decide("w", defaults, defaults, presets))
    }

    @Test
    fun `any preset can be the auto one`() {
        assertEquals(Decision.Apply("tall"), auto.withPreset("tall").decide("w", defaults, defaults, presets))
    }

    @Test
    fun `the file keeps the choice, including none, and the worlds`() {
        val saved = auto.withPreset(null).remember("server:example.com", "tall").remember("world:x", AttributeAutoApply.CUSTOM)
        assertEquals(saved, AttributeAutoApply.parse(saved.serialize()))
        assertNull(AttributeAutoApply.parse(saved.serialize()).preset)
        assertEquals("boosted", AttributeAutoApply.parse("{}").preset)
        assertEquals(AttributeAutoApply(), AttributeAutoApply.parse("not json ["))
    }
}
