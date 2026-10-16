package dev.luniiya.mctinytweak.mcp

import com.google.gson.JsonObject
import dev.luniiya.mctinytweak.client.mcp.GameTools
import dev.luniiya.mctinytweak.client.mcp.Mcp
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ChatToolsTest {
    @Test
    fun `pending MCP wait resumes on incoming chat without including tool feedback`() {
        val id = Mcp.events.create()
        val after = Mcp.chat.lastId()
        val started = CountDownLatch(1)
        try {
            val waiting = CompletableFuture.supplyAsync {
                Mcp.events.withSession(id) {
                    started.countDown()
                    GameTools.all.single { it.name == "wait_for_chat" }.call(JsonObject().apply {
                        addProperty("after_id", after); addProperty("timeout_ms", 2000)
                    })
                }
            }
            assertTrue(started.await(1, TimeUnit.SECONDS))
            Mcp.chat.add("[Baritone] feedback")
            Mcp.chat.add("<Player1> chat wake regression", wakeable = true)
            val result = waiting.get(3, TimeUnit.SECONDS)
            val text = (result.content.single() as ToolResult.Content.Text).text
            assertTrue(text.contains("chat wake regression"))
            assertFalse(text.contains("feedback"))
        } finally { Mcp.events.remove(id) }
    }

    @Test
    fun `a cursor from before a game restart does not hide new chat`() {
        val id = Mcp.events.create()
        try {
            Mcp.chat.add("<Player1> message after restart", wakeable = true)
            Mcp.events.withSession(id) {
                val result = GameTools.all.single { it.name == "wait_for_chat" }.call(JsonObject().apply {
                    addProperty("after_id", Int.MAX_VALUE); addProperty("timeout_ms", 0)
                })
                assertTrue((result.content.single() as ToolResult.Content.Text).text.contains("message after restart"))
            }
        } finally { Mcp.events.remove(id) }
    }

    @Test
    fun `an agent can mute its own pending wake and anonymous waits are refused`() {
        val tool = GameTools.all.single { it.name == "wait_for_chat" }
        assertThrows(ToolError::class.java) { tool.call(JsonObject().apply { addProperty("timeout_ms", 0) }) }
        val id = Mcp.events.create()
        try {
            Mcp.events.withSession(id) {
                GameTools.all.single { it.name == "chat_notifications" }.call(JsonObject().apply { addProperty("enabled", false) })
                val result = tool.call(JsonObject().apply { addProperty("timeout_ms", 2000) })
                assertEquals("Chat wake is muted", (result.content.single() as ToolResult.Content.Text).text)
            }
        } finally { Mcp.events.remove(id) }
    }
}
