package dev.luniiya.mctinytweak.attributes

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Saved sets of base attribute values, by name. [DEFAULT] is built in: it is never stored
 * and loading it resets every attribute.
 */
data class AttributePresets(val presets: Map<String, Map<String, Double>> = emptyMap()) {
    val names: List<String> get() = listOf(DEFAULT) + presets.keys.sortedBy { it.lowercase() }

    fun save(name: String, values: Map<String, Double>): AttributePresets {
        val clean = name.trim()
        require(isValidName(clean)) { "Preset name must not be empty or $DEFAULT" }
        // Replace any preset whose name only differs in case, so names stay unique.
        val others = presets.filterKeys { !it.equals(clean, ignoreCase = true) }
        return copy(presets = others + (clean to values.filterValues { it.isFinite() }))
    }

    fun delete(name: String) = copy(presets = presets - name)

    /**
     * Commands that turn [current] base values into the preset's. Attributes the preset does not
     * mention are reset, and attributes already at their target are skipped to keep chat quiet.
     * [defaults] holds each attribute's reset value.
     */
    fun loadCommands(name: String, current: Map<String, Double>, defaults: Map<String, Double>): List<String> {
        val target = if (name == DEFAULT) emptyMap() else presets[name] ?: return emptyList()
        return current.keys.sorted().mapNotNull { id ->
            val wanted = target[id]
            when {
                wanted != null -> if (current[id] == wanted) null else AttributeCommands.setBase(id, wanted)
                current[id] == defaults[id] -> null
                else -> AttributeCommands.resetBase(id)
            }
        }
    }

    fun serialize(): String {
        val root = JsonObject()
        for ((name, values) in presets.toSortedMap()) {
            root.add(name, JsonObject().apply { values.toSortedMap().forEach { (id, value) -> addProperty(id, value) } })
        }
        return GSON.toJson(root) + "\n"
    }

    companion object {
        const val DEFAULT = "Default"
        const val FILE_NAME = "mc_tiny_tweak_attribute_presets.json"
        private val GSON = GsonBuilder().setPrettyPrinting().create()

        fun isValidName(name: String) = name.isNotBlank() && !name.trim().equals(DEFAULT, ignoreCase = true)

        /** Reads presets, skipping anything that is not a name mapped to numbers. */
        fun parse(text: String): AttributePresets {
            val root = JsonParser.parseString(text).takeIf { it.isJsonObject }?.asJsonObject ?: return AttributePresets()
            val presets = root.entrySet()
                .filter { (name, value) -> isValidName(name) && value.isJsonObject }
                .associate { (name, value) ->
                    name to value.asJsonObject.entrySet()
                        .mapNotNull { (id, number) ->
                            val v = number.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble
                            v?.takeIf { it.isFinite() }?.let { id to it }
                        }
                        .toMap()
                }
            return AttributePresets(presets)
        }
    }
}
