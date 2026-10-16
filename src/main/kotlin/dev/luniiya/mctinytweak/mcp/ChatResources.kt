package dev.luniiya.mctinytweak.mcp

import com.google.gson.JsonArray
import com.google.gson.JsonObject

class ChatResources(private val chat: ChatLog, private val events: McpChatEvents, private val allowed: () -> Boolean) {
    fun visible() = allowed()
    fun handle(method: String, params: JsonObject): JsonObject {
        if (method == "resources/list") return JsonObject().apply {
            add("resources", JsonArray().apply {
                if (allowed()) add(JsonObject().apply {
                    addProperty("uri", McpChatEvents.URI)
                    addProperty("name", "Minecraft chat")
                    addProperty("description", "Recent chat. Incoming public player messages notify subscribed clients; hosts decide whether to wake an agent.")
                    addProperty("mimeType", "text/plain")
                })
            })
        }
        if (!allowed()) throw ToolError("Reading chat is turned off")
        val uri = params.get("uri")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
        if (uri != McpChatEvents.URI) throw ToolError("Unknown chat resource URI")
        return when (method) {
            "resources/read" -> JsonObject().apply {
                add("contents", JsonArray().apply { add(JsonObject().apply {
                    addProperty("uri", McpChatEvents.URI)
                    addProperty("mimeType", "text/plain")
                    addProperty("text", chat.after(0, 100).joinToString("\n") { "[${it.id}] ${it.text}" })
                }) })
            }
            "resources/subscribe", "resources/unsubscribe" -> {
                events.subscribe(method == "resources/subscribe")
                JsonObject()
            }
            else -> throw ToolError("Unknown resource method")
        }
    }
}
