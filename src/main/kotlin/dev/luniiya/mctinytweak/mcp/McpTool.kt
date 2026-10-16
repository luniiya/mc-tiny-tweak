package dev.luniiya.mctinytweak.mcp

import com.google.gson.JsonArray
import com.google.gson.JsonObject

/** One MCP tool. [call] runs on the server's request thread and may throw [ToolError] for a readable failure. */
class McpTool(
    val name: String,
    val description: String,
    val feature: McpSettings.Feature,
    val inputSchema: JsonObject = Schema.obj(),
    val call: (JsonObject) -> ToolResult,
)

/** A failure the agent should read, reported as an error result rather than a protocol error. */
class ToolError(message: String) : Exception(message)

data class ToolResult(val content: List<Content>, val isError: Boolean = false) {
    sealed interface Content {
        data class Text(val text: String) : Content
        data class Image(val base64: String, val mimeType: String) : Content
    }

    fun toJson() = JsonObject().apply {
        add("content", JsonArray().apply {
            for (item in content) add(
                JsonObject().apply {
                    when (item) {
                        is Content.Text -> { addProperty("type", "text"); addProperty("text", item.text) }
                        is Content.Image -> { addProperty("type", "image"); addProperty("data", item.base64); addProperty("mimeType", item.mimeType) }
                    }
                },
            )
        })
        addProperty("isError", isError)
    }

    companion object {
        fun text(text: String) = ToolResult(listOf(Content.Text(text)))
        fun error(text: String) = ToolResult(listOf(Content.Text(text)), isError = true)
    }
}

/** Small builders for JSON Schemas and for reading tool arguments. */
object Schema {
    data class Property(val name: String, val schema: JsonObject, val required: Boolean)

    fun obj(vararg properties: Property) = JsonObject().apply {
        addProperty("type", "object")
        add("properties", JsonObject().apply { for (p in properties) add(p.name, p.schema) })
        val required = properties.filter { it.required }
        if (required.isNotEmpty()) add("required", JsonArray().apply { required.forEach { add(it.name) } })
    }

    fun string(name: String, description: String, required: Boolean = false) =
        Property(name, typed("string", description), required)

    fun integer(name: String, description: String, required: Boolean = false, min: Int? = null, max: Int? = null) =
        Property(name, typed("integer", description).apply {
            min?.let { addProperty("minimum", it) }
            max?.let { addProperty("maximum", it) }
        }, required)

    fun number(name: String, description: String, required: Boolean = false) =
        Property(name, typed("number", description), required)

    fun stringList(name: String, description: String, required: Boolean = false) =
        Property(name, typed("array", description).apply { add("items", JsonObject().apply { addProperty("type", "string") }) }, required)

    private fun typed(type: String, description: String) = JsonObject().apply {
        addProperty("type", type)
        addProperty("description", description)
    }

    fun JsonObject.requireString(name: String): String =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString ?: throw ToolError("Missing text argument: $name")

    fun JsonObject.optInt(name: String, default: Int, range: IntRange): Int {
        val value = get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble ?: return default
        if (value % 1.0 != 0.0 || value.toLong() !in range.first..range.last) throw ToolError("$name must be a whole number between ${range.first} and ${range.last}")
        return value.toInt()
    }

    fun JsonObject.optDouble(name: String): Double? =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble

    fun JsonObject.requireDouble(name: String): Double = optDouble(name) ?: throw ToolError("Missing number argument: $name")

    fun JsonObject.stringList(name: String): List<String> {
        val array = get(name)?.takeIf { it.isJsonArray }?.asJsonArray ?: throw ToolError("Missing list argument: $name")
        return array.map { if (it.isJsonPrimitive && it.asJsonPrimitive.isString) it.asString else throw ToolError("$name must only hold text") }
    }
}
