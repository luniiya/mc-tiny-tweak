package dev.luniiya.mctinytweak.mcp

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ToolCallSummaryTest {
    private fun args(json: String) = JsonParser.parseString(json).asJsonObject

    @Test
    fun `calls read as the tool name and plain arguments`() {
        assertEquals("get_state", ToolCallSummary.describe("get_state", args("{}")))
        assertEquals("hold_keys keys=[forward, jump], ticks=40", ToolCallSummary.describe("hold_keys", args("""{"keys":["forward","jump"],"ticks":40}""")))
        assertEquals("run_command command=/time set day", ToolCallSummary.describe("run_command", args("""{"command":"/time set day"}""")))
    }

    @Test
    fun `long calls are cut to fit one chat line`() {
        val line = ToolCallSummary.describe("send_chat", args("""{"message":"${"a".repeat(500)}"}"""))
        assertEquals(ToolCallSummary.MAX_LENGTH, line.length)
        assertTrue(line.endsWith("..."))
    }
}
