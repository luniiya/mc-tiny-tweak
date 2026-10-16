package dev.luniiya.mctinytweak.mcp

import com.google.gson.JsonObject
import dev.luniiya.mctinytweak.client.mcp.GameTools
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ContainerToolsTest {
    @Test
    fun `respawn requires control before accessing the game`() {
        val tool = GameTools.all.single { it.name == "respawn" }
        assertEquals(McpSettings.Feature.INPUT, tool.feature)
        assertTrue(assertThrows(ToolError::class.java) { tool.call(JsonObject()) }.message!!.contains("take_over"))
    }

    @Test
    fun `container inspection uses state permission and clicks require input permission and takeover`() {
        val inspect = GameTools.all.single { it.name == "get_container" }
        val click = GameTools.all.single { it.name == "click_slot" }
        assertEquals(McpSettings.Feature.STATE, inspect.feature)
        assertEquals(McpSettings.Feature.INPUT, click.feature)
        assertEquals(setOf("container_id", "slot"), click.inputSchema["required"].asJsonArray.map { it.asString }.toSet())
        val error = assertThrows(ToolError::class.java) {
            click.call(JsonObject().apply { addProperty("container_id", 0); addProperty("slot", 0) })
        }
        assertTrue(error.message!!.contains("take_over"))
    }
}
