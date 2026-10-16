package dev.luniiya.mctinytweak.mcp

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * The JSON-RPC side of MCP: initialize, ping, tools/list and tools/call. Only tools whose feature
 * [allowed] accepts are listed or callable, so switching a feature off takes effect on the next request.
 */
class McpProtocol(
    private val serverName: String,
    private val serverVersion: String,
    private val tools: () -> List<McpTool>,
    private val allowed: (McpSettings.Feature) -> Boolean,
    private val onError: (String, Throwable) -> Unit = { _, _ -> },
    /** Called before an allowed tool runs, with its arguments. */
    private val onCall: (McpTool, JsonObject) -> Unit = { _, _ -> },
) {
    /** Answers one request. Returns null for notifications, which get no reply. */
    fun handle(body: String): String? {
        val message = try {
            JsonParser.parseString(body)
        } catch (e: Exception) {
            return error(JsonNull.INSTANCE, PARSE_ERROR, "Invalid JSON").toString()
        }
        if (!message.isJsonObject) return error(JsonNull.INSTANCE, INVALID_REQUEST, "Expected one JSON-RPC object").toString()
        val request = message.asJsonObject
        val method = request.get("method")?.takeIf { it.isJsonPrimitive }?.asString
        val id = request.get("id")
        if (id == null || id.isJsonNull) return null
        if (method == null) return error(id, INVALID_REQUEST, "Missing method").toString()
        val params = request.get("params")?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
        val response = when (method) {
            "initialize" -> result(id, initialize(params))
            "ping" -> result(id, JsonObject())
            "tools/list" -> result(id, listTools())
            "tools/call" -> callTool(id, params)
            else -> error(id, METHOD_NOT_FOUND, "Unknown method: $method")
        }
        return response.toString()
    }

    private fun initialize(params: JsonObject) = JsonObject().apply {
        val requested = params.get("protocolVersion")?.takeIf { it.isJsonPrimitive }?.asString
        addProperty("protocolVersion", requested?.takeIf { it in SUPPORTED_VERSIONS } ?: SUPPORTED_VERSIONS.first())
        add("capabilities", JsonObject().apply { add("tools", JsonObject()) })
        add("serverInfo", JsonObject().apply {
            addProperty("name", serverName)
            addProperty("version", serverVersion)
        })
        addProperty("instructions", INSTRUCTIONS)
    }

    private fun listTools() = JsonObject().apply {
        add("tools", com.google.gson.JsonArray().apply {
            for (tool in tools().filter { allowed(it.feature) }) add(
                JsonObject().apply {
                    addProperty("name", tool.name)
                    addProperty("description", tool.description)
                    add("inputSchema", tool.inputSchema)
                },
            )
        })
    }

    private fun callTool(id: JsonElement, params: JsonObject): JsonObject {
        val name = params.get("name")?.takeIf { it.isJsonPrimitive }?.asString ?: return error(id, INVALID_PARAMS, "Missing tool name")
        val tool = tools().firstOrNull { it.name == name } ?: return error(id, INVALID_PARAMS, "Unknown tool: $name")
        if (!allowed(tool.feature)) return result(id, ToolResult.error("$name is turned off in the game's MCP settings").toJson())
        val arguments = params.get("arguments")?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
        val outcome = try {
            onCall(tool, arguments)
            tool.call(arguments)
        } catch (e: ToolError) {
            ToolResult.error(e.message ?: "Failed")
        } catch (e: Exception) {
            onError(name, e)
            ToolResult.error("$name failed: ${e.message ?: e.javaClass.simpleName}")
        }
        return result(id, outcome.toJson())
    }

    private fun result(id: JsonElement, value: JsonObject) = envelope(id).apply { add("result", value) }

    private fun error(id: JsonElement, code: Int, message: String) = envelope(id).apply {
        add("error", JsonObject().apply {
            addProperty("code", code)
            addProperty("message", message)
        })
    }

    private fun envelope(id: JsonElement) = JsonObject().apply {
        addProperty("jsonrpc", "2.0")
        add("id", id)
    }

    companion object {
        /** Newest first; the first one answers clients that ask for a version this server does not know. */
        val SUPPORTED_VERSIONS = listOf("2025-11-25", "2025-06-18", "2025-03-26", "2024-11-05")
        const val PARSE_ERROR = -32700
        const val INVALID_REQUEST = -32600
        const val METHOD_NOT_FOUND = -32601
        const val INVALID_PARAMS = -32602

        const val INSTRUCTIONS = "Controls a running Minecraft client as its player. Start with get_state, and take a screenshot " +
            "to see the game. Commands and their feedback go through run_command; read_chat shows what others say. " +
            "Movement uses hold_keys for a number of ticks (20 ticks is one second). take_over gives you sole control " +
            "and is required for send_chat, Baritone commands and joining or leaving worlds; release_control hands it back. " +
            "Baritone settings can be read and changed without it. While you are in control the person watches in freecam; " +
            "screenshot with view player shows what they see."
    }
}
