package dev.luniiya.mctinytweak.mcp

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WorldCreationSpecTest {
    private fun spec(id: String = "Sol-MCP-Test", mode: String = "creative", difficulty: String = "peaceful") =
        WorldCreationSpec(id, "Sol MCP test", mode, difficulty, true, true, "123")

    @Test
    fun `world ids cannot escape the saves directory or reference existing path components`() {
        for (id in listOf("../run-server", "..", "/tmp/world", "a/b", "a\\b", "", ".hidden", "a".repeat(65)))
            assertThrows(IllegalArgumentException::class.java) { spec(id) }
        assertEquals("Sol-MCP-Test", spec().id)
    }

    @Test
    fun `creation settings are explicit and validated`() {
        val creative = spec()
        assertTrue(creative.commands)
        assertTrue(creative.flat)
        assertEquals("creative", creative.mode)
        assertThrows(IllegalArgumentException::class.java) { spec(mode = "creativ") }
        assertThrows(IllegalArgumentException::class.java) { spec(difficulty = "peacefull") }
        assertThrows(IllegalArgumentException::class.java) { creative.copy(name = "\n") }
        assertThrows(IllegalArgumentException::class.java) { creative.copy(seed = "1".repeat(129)) }
    }
}
