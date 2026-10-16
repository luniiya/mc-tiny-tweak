package dev.luniiya.mctinytweak.mcp

import dev.luniiya.mctinytweak.mcp.McpSettings.Feature
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class McpSettingsTest {
    @Test
    fun `the server and every feature are on by default`() {
        for (settings in listOf(McpSettings(), McpSettings.parse(""))) {
            assertTrue(settings.enabled)
            assertEquals(McpSettings.DEFAULT_PORT, settings.port)
            assertTrue(Feature.entries.all(settings::allows))
            assertTrue(settings.showActions)
        }
    }

    @Test
    fun `settings survive saving and loading`() {
        val custom = McpSettings(enabled = false, port = 30000, features = setOf(Feature.STATE, Feature.CHAT_READ), showActions = false)
        assertEquals(custom, McpSettings.parse(custom.serialize()))
    }

    @Test
    fun `switching one feature leaves the others alone`() {
        val noCommands = McpSettings().with(Feature.COMMANDS, false)
        assertFalse(noCommands.allows(Feature.COMMANDS))
        assertEquals(Feature.entries.size - 1, noCommands.features.size)
        assertTrue(noCommands.with(Feature.COMMANDS, true).allows(Feature.COMMANDS))
    }

    @Test
    fun `bad values and unknown keys fall back to defaults`() {
        val parsed = McpSettings.parse("# comment\nenabled=perhaps\nport=80\nfeature.input=nope\nfeature.unknown=false\n")
        assertEquals(McpSettings(), parsed)
        assertFalse(McpSettings.parse("feature.screenshot = false").allows(Feature.SCREENSHOT))
    }

    @Test
    fun `feature keys are unique`() {
        assertEquals(Feature.entries.size, Feature.entries.map { it.key }.toSet().size)
    }

    @Test
    fun `the port a game got is remembered for its next start`() {
        val settings = McpSettings()
        assertSame(settings, settings.rememberingPort(McpSettings.DEFAULT_PORT))
        val moved = settings.rememberingPort(25603)
        assertEquals(25603, moved.port)
        assertEquals(25603, McpSettings.parse(moved.serialize()).port)
        assertEquals(settings.copy(port = 25603), moved, "nothing else changes")
    }

    @Test
    fun `a lot of games can run at once, each under its own name`() {
        assertTrue(McpSettings.PORT_ATTEMPTS >= 50)
        assertEquals("minecraft", McpSettings.serverName(McpSettings.DEFAULT_PORT))
        val names = (0 until McpSettings.PORT_ATTEMPTS).map { McpSettings.serverName(McpSettings.DEFAULT_PORT + it) }
        assertEquals(names.size, names.toSet().size)
        assertEquals("minecraft-25600", McpSettings.serverName(25600))
    }
}
