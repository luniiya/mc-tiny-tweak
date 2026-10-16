package dev.luniiya.mctinytweak.baritone

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BaritoneDefaultsTest {
    @Test
    fun `parses Baritone's settings format and skips comments`() {
        val parsed = BaritoneDefaults.parse("# comment\n\nallowParkour true\n  blocksToAvoid  stone,dirt \nbroken\n")
        assertEquals(mapOf("allowparkour" to "true", "blockstoavoid" to "stone,dirt"), parsed)
    }

    @Test
    fun `the shipped defaults load and only use known setting shapes`() {
        val defaults = BaritoneDefaults.load()
        assertEquals("true", defaults["allowinventory"])
        assertEquals("true", defaults["autotool"])
        assertEquals("false", defaults["assumeexternalautotool"])
        assertTrue(defaults.keys.all { it == it.lowercase() && ' ' !in it })
    }

    @Test
    fun `reset uses the mod's default when it has one`() {
        val mod = mapOf("allowinventory" to "true")
        assertEquals("true", BaritoneDefaults.defaultFor("allowInventory", "false", mod))
        assertEquals("false", BaritoneDefaults.defaultFor("allowParkour", "false", mod))
    }

    @Test
    fun `defaults added later reach instances that already got the old ones`() {
        val defaults = mapOf("allowinventory" to "true", "freelook" to "false")
        assertEquals(defaults, BaritoneDefaults.notYetApplied(defaults, null), "a new instance gets every default")
        val marker = BaritoneDefaults.markerText(listOf("allowinventory"))
        assertEquals(mapOf("freelook" to "false"), BaritoneDefaults.notYetApplied(defaults, marker))
        assertEquals(emptyMap<String, String>(), BaritoneDefaults.notYetApplied(defaults, BaritoneDefaults.markerText(defaults.keys)))
    }

    @Test
    fun `the shipped defaults turn off Baritone's free look`() {
        assertEquals("false", BaritoneDefaults.load()["freelook"], "the player must look where Baritone goes")
    }

    @Test
    fun `only true and false count as on or off values`() {
        assertTrue(BaritoneDefaults.isBooleanText("TRUE"))
        assertTrue(BaritoneDefaults.isBooleanText(" false "))
        assertFalse(BaritoneDefaults.isBooleanText("maybe"), "Baritone itself would quietly read this as false")
        assertFalse(BaritoneDefaults.isBooleanText("1"))
    }

    @Test
    fun `search needs every word, ignoring case`() {
        assertTrue(BaritoneDefaults.matches("allowParkourPlace", "parkour PLACE"))
        assertFalse(BaritoneDefaults.matches("allowParkour", "parkour place"))
        assertTrue(BaritoneDefaults.matches("anything", "  "))
    }
}

class BaritoneToolsTest {
    @Test
    fun `agents can read, change and reset Baritone settings`() {
        val names = dev.luniiya.mctinytweak.client.mcp.GameTools.all.associateBy { it.name }
        for (tool in listOf("baritone_settings", "baritone_set_setting", "baritone_reset_settings")) {
            assertEquals(dev.luniiya.mctinytweak.mcp.McpSettings.Feature.BARITONE, names[tool]?.feature, tool)
        }
    }

    @Test
    fun `the Baritone settings screen has its text`() {
        val container = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(dev.luniiya.mctinytweak.McTinyTweak.MOD_ID).orElseThrow()
        val lang = java.nio.file.Files.newBufferedReader(container.findPath("assets/mc_tiny_tweak/lang/en_us.json").orElseThrow())
            .use { com.google.gson.JsonParser.parseReader(it).asJsonObject }
        for (key in listOf("", ".tooltip", ".missing", ".search", ".reset", ".reset.tooltip", ".reset.confirm", ".setting.tooltip")) {
            assertTrue(lang.has("screen.mc_tiny_tweak.baritone$key"), key)
        }
    }
}
