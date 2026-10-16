package dev.luniiya.mctinytweak.mcp

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/** One short line describing a tool call, shown in chat so the player can follow what the agent does. */
object ToolCallSummary {
    const val MAX_LENGTH = 120

    fun describe(tool: String, arguments: JsonObject): String {
        val args = arguments.entrySet().joinToString(", ") { (name, value) -> "$name=${plain(value)}" }
        val line = if (args.isEmpty()) tool else "$tool $args"
        return if (line.length <= MAX_LENGTH) line else line.take(MAX_LENGTH - 3) + "..."
    }

    private fun plain(value: JsonElement): String = when {
        value.isJsonArray -> value.asJsonArray.joinToString(", ", "[", "]") { plain(it) }
        value.isJsonPrimitive -> value.asString
        else -> value.toString()
    }
}
