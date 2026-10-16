package dev.luniiya.mctinytweak.mcp

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.Socket
import java.net.ServerSocket

class McpChatEventsTest {
    @Test
    fun `incoming waits exclude own echoes and tool feedback without losing read chat history`() {
        val chat = ChatLog()
        chat.add("<Sol> own message")
        val incoming = chat.add("<Player1> hello", wakeable = true)
        chat.add("[Baritone] pathing")
        assertEquals(3, chat.after(0, 50).size)
        assertEquals(listOf(incoming), chat.incomingAfter(0, 50))
        assertTrue(chat.incomingAfter(incoming.id, 50).isEmpty())
    }

    @Test
    fun `pending waits see their own mute state`() {
        val events = McpChatEvents { true }
        val id = events.create()
        events.withSession(id) {
            assertFalse(events.muted())
            events.wake(false)
            assertTrue(events.muted())
            events.wake(true)
            assertFalse(events.muted())
        }
    }

    @Test
    fun `only incoming public chat wakes an agent`() {
        val filter = ChatWakeFilter()
        filter.sentByAgent("hello")
        assertTrue(filter.isIncoming("<Player1> hey Sol", "Sol"))
        for (text in listOf("<Sol> hello", "[Baritone] mining", "Gave Sol iron", "Player1 joined the game", "[MC Tiny Tweak] action")) {
            assertFalse(filter.isIncoming(text, "Sol"), text)
        }
        assertFalse(filter.isIncoming("<Player1> hello", null))
    }

    @Test
    fun `human chat through the local player wakes while exact agent echoes do not`() {
        val filter = ChatWakeFilter()
        filter.sentByAgent("I am building")
        assertTrue(filter.isIncoming("<Sol> damn impressive", "Sol"))
        assertFalse(filter.isIncoming("<Sol> I am building", "Sol"))
        assertTrue(filter.isIncoming("<Sol> I am building", "Sol"), "A later human repetition must wake")
    }

    @Test
    fun `undelivered agent echoes expire and the pending echo list is bounded`() {
        var now = 0L
        val filter = ChatWakeFilter { now }
        filter.sentByAgent("lost")
        now = 30_000
        assertTrue(filter.isIncoming("<Sol> lost", "Sol"))
        repeat(33) { filter.sentByAgent("message $it") }
        assertTrue(filter.isIncoming("<Sol> message 0", "Sol"))
        assertFalse(filter.isIncoming("<Sol> message 32", "Sol"))
    }

    @Test
    fun `subscriptions and muting belong to each initialized client`() {
        val events = McpChatEvents { true }
        val first = events.create(); val second = events.create()
        events.withSession(first) { events.subscribe(true) }
        events.publishChat()
        assertNotNull(events.poll(first, 0))
        assertNull(events.poll(second, 0))
        events.withSession(second) { events.subscribe(true) }
        events.withSession(first) { events.wake(false) }
        events.publishChat()
        assertNull(events.poll(first, 0))
        assertNotNull(events.poll(second, 0))
        events.withSession(first) { events.wake(true); events.subscribe(false) }
        events.publishChat()
        assertNull(events.poll(first, 0))
    }

    @Test
    fun `muting discards queued events and reenable does not replay muted messages`() {
        var enabled = true
        val events = McpChatEvents { enabled }
        val id = events.create()
        events.withSession(id) { events.subscribe(true) }
        repeat(50) { events.publishChat() }
        assertNotNull(events.poll(id, 0)); assertNull(events.poll(id, 0), "bursts coalesce")
        events.publishChat()
        events.withSession(id) { events.wake(false) }
        events.publishChat()
        events.withSession(id) { events.wake(true) }
        assertNull(events.poll(id, 0))
        enabled = false; events.publishChat(); enabled = true
        assertNull(events.poll(id, 0))
        assertThrows(ToolError::class.java) { events.subscribe(true) }
    }

    @Test
    fun `chat resources respect read permissions and do not subscribe anonymous requests`() {
        var allowed = true
        val log = ChatLog(); log.add("<Player1> hi")
        val events = McpChatEvents { true }
        val resources = ChatResources(log, events) { allowed }
        val uri = JsonObject().apply { addProperty("uri", McpChatEvents.URI) }
        assertEquals(1, resources.handle("resources/list", JsonObject())["resources"].asJsonArray.size())
        assertTrue(resources.handle("resources/read", uri)["contents"].asJsonArray[0].asJsonObject["text"].asString.contains("hi"))
        assertThrows(ToolError::class.java) { resources.handle("resources/subscribe", uri) }
        val id = events.create()
        events.withSession(id) { resources.handle("resources/subscribe", uri) }
        events.publishChat(); assertNotNull(events.poll(id, 0))
        allowed = false
        assertEquals(0, resources.handle("resources/list", JsonObject())["resources"].asJsonArray.size())
        assertThrows(ToolError::class.java) { resources.handle("resources/read", uri) }
    }

    @Test
    fun `wake setting persists and old settings files keep a compatible default`() {
        assertTrue(McpSettings.parse("enabled=true").wakeOnChat)
        val settings = McpSettings(wakeOnChat = false)
        assertEquals(settings, McpSettings.parse(settings.serialize()))
    }

    @Test
    fun `authenticated GET delivers a standard resource update and stop closes the stream`() {
        val events = McpChatEvents { true }
        val session = events.create()
        events.withSession(session) { events.subscribe(true) }
        val server = McpHttpServer("test-token") { "{}" }.apply { chatEvents = events }
        val port = server.start(ServerSocket(0).use { it.localPort })
        try {
            Socket("127.0.0.1", port).use { socket ->
                socket.soTimeout = 3000
                socket.getOutputStream().write(("GET /mcp HTTP/1.1\r\nHost: 127.0.0.1:$port\r\nAuthorization: Bearer test-token\r\nMcp-Session-Id: $session\r\nAccept: text/event-stream\r\n\r\n").toByteArray())
                val reader = socket.getInputStream().bufferedReader()
                assertEquals("HTTP/1.1 200 OK", reader.readLine())
                while (reader.readLine() != "") { }
                assertEquals(": connected", reader.readLine()); assertEquals("", reader.readLine())
                events.publishChat()
                assertEquals("event: message", reader.readLine())
                val json = JsonParser.parseString(reader.readLine().removePrefix("data: ")).asJsonObject
                assertEquals("notifications/resources/updated", json["method"].asString)
                assertEquals(McpChatEvents.URI, json["params"].asJsonObject["uri"].asString)
                assertEquals("", reader.readLine())
                server.stop()
                assertNull(reader.readLine())
            }
        } finally { server.stop() }
    }

    @Test
    fun `initialize announces subscribable resources and returns an isolated HTTP session`() {
        val events = McpChatEvents { true }
        val protocol = McpProtocol("test", "1", { emptyList() }, { true }, chatResources = ChatResources(ChatLog(), events) { true })
        val server = McpHttpServer("test-token", protocol::handle).apply { chatEvents = events }
        val port = server.start(ServerSocket(0).use { it.localPort })
        try {
            val body = """{"jsonrpc":"2.0","id":1,"method":"initialize"}"""
            Socket("127.0.0.1", port).use { socket ->
                socket.soTimeout = 3000
                socket.getOutputStream().write(("POST /mcp HTTP/1.1\r\nHost: localhost:$port\r\nAuthorization: Bearer test-token\r\nContent-Length: ${body.length}\r\n\r\n$body").toByteArray())
                val response = socket.getInputStream().readBytes().toString(Charsets.UTF_8)
                val session = response.lineSequence().single { it.startsWith("Mcp-Session-Id:") }.substringAfter(':').trim()
                assertTrue(events.exists(session))
                val result = JsonParser.parseString(response.substringAfter("\r\n\r\n")).asJsonObject["result"].asJsonObject
                assertTrue(result["capabilities"].asJsonObject["resources"].asJsonObject["subscribe"].asBoolean)
            }
        } finally { server.stop() }
    }
}
