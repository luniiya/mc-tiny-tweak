package dev.luniiya.mctinytweak.mcp

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.luniiya.mctinytweak.mcp.McpSettings.Feature
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class McpProtocolTest {
    private var allowed = Feature.entries.toSet()
    private val tools = listOf(
        McpTool("echo", "Echoes text", Feature.CHAT_SEND, Schema.obj(Schema.string("text", "Text", required = true))) { args ->
            with(Schema) { ToolResult.text(args.requireString("text")) }
        },
        McpTool("fail", "Throws", Feature.STATE) { throw IllegalStateException("boom") },
        McpTool("refuse", "Refuses", Feature.STATE) { throw ToolError("not now") },
    )
    private val errors = mutableListOf<String>()
    private val protocol = McpProtocol("test", "1", { tools }, { it in allowed }, { name, _ -> errors += name })

    private fun call(method: String, params: String = "{}", id: Int = 1): JsonObject =
        JsonParser.parseString(protocol.handle("""{"jsonrpc":"2.0","id":$id,"method":"$method","params":$params}""")!!).asJsonObject

    @Test
    fun `initialize echoes a known protocol version and announces tools`() {
        val result = call("initialize", """{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"c","version":"1"}}""")["result"].asJsonObject
        assertEquals("2025-06-18", result["protocolVersion"].asString)
        assertTrue(result["capabilities"].asJsonObject.has("tools"))
        assertEquals("test", result["serverInfo"].asJsonObject["name"].asString)
        val unknown = call("initialize", """{"protocolVersion":"1999-01-01"}""")["result"].asJsonObject
        assertEquals(McpProtocol.SUPPORTED_VERSIONS.first(), unknown["protocolVersion"].asString)
    }

    @Test
    fun `notifications get no reply and the id is echoed back`() {
        assertNull(protocol.handle("""{"jsonrpc":"2.0","method":"notifications/initialized"}"""))
        assertEquals("abc", JsonParser.parseString(protocol.handle("""{"jsonrpc":"2.0","id":"abc","method":"ping"}""")).asJsonObject["id"].asString)
    }

    @Test
    fun `tools list hides switched off features`() {
        fun names() = call("tools/list")["result"].asJsonObject["tools"].asJsonArray.map { it.asJsonObject["name"].asString }
        assertEquals(listOf("echo", "fail", "refuse"), names())
        allowed = allowed - Feature.CHAT_SEND
        assertEquals(listOf("fail", "refuse"), names())
    }

    @Test
    fun `tool calls return content, and switched off tools refuse`() {
        val result = call("tools/call", """{"name":"echo","arguments":{"text":"hi"}}""")["result"].asJsonObject
        assertFalse(result["isError"].asBoolean)
        assertEquals("hi", result["content"].asJsonArray[0].asJsonObject["text"].asString)
        allowed = emptySet()
        assertTrue(call("tools/call", """{"name":"echo","arguments":{"text":"hi"}}""")["result"].asJsonObject["isError"].asBoolean)
    }

    @Test
    fun `tool failures become error results and unexpected ones are reported`() {
        val refused = call("tools/call", """{"name":"refuse"}""")["result"].asJsonObject
        assertTrue(refused["isError"].asBoolean)
        assertEquals("not now", refused["content"].asJsonArray[0].asJsonObject["text"].asString)
        assertTrue(call("tools/call", """{"name":"fail"}""")["result"].asJsonObject["isError"].asBoolean)
        assertTrue(call("tools/call", """{"name":"echo","arguments":{}}""")["result"].asJsonObject["isError"].asBoolean, "missing argument")
        assertEquals(listOf("fail"), errors)
    }

    @Test
    fun `protocol errors use JSON-RPC codes`() {
        assertEquals(McpProtocol.METHOD_NOT_FOUND, call("nope")["error"].asJsonObject["code"].asInt)
        assertEquals(McpProtocol.INVALID_PARAMS, call("tools/call", """{"name":"missing"}""")["error"].asJsonObject["code"].asInt)
        val parse = JsonParser.parseString(protocol.handle("{not json")).asJsonObject
        assertEquals(McpProtocol.PARSE_ERROR, parse["error"].asJsonObject["code"].asInt)
    }

    @Test
    fun `image results carry base64 data and a mime type`() {
        val json = ToolResult(listOf(ToolResult.Content.Image("AAAA", "image/png"))).toJson()
        val item = json["content"].asJsonArray[0].asJsonObject
        assertEquals("image", item["type"].asString)
        assertEquals("AAAA", item["data"].asString)
        assertEquals("image/png", item["mimeType"].asString)
    }
}
