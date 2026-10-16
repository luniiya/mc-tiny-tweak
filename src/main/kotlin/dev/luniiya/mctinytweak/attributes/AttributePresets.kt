package dev.luniiya.mctinytweak.attributes

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Saved sets of base attribute values, by name. [DEFAULT] and the [builtIn] presets ship with the
 * mod: they are never stored, cannot be saved over, and loading [DEFAULT] resets every attribute.
 */
data class AttributePresets(
    val presets: Map<String, Map<String, Double>> = emptyMap(),
    val builtIn: Map<String, Map<String, Double>> = emptyMap(),
) {
    val names: List<String>
        get() = listOf(DEFAULT) + builtIn.keys.sortedBy { it.lowercase() } +
            presets.keys.filter { !isBuiltIn(it) }.sortedBy { it.lowercase() }

    fun isBuiltIn(name: String) = name.trim().equals(DEFAULT, ignoreCase = true) ||
        builtIn.keys.any { it.equals(name.trim(), ignoreCase = true) }

    fun canSave(name: String) = name.isNotBlank() && !isBuiltIn(name)

    fun save(name: String, values: Map<String, Double>): AttributePresets {
        val clean = name.trim()
        require(canSave(clean)) { "Preset name must not be empty or a built-in preset" }
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
        val target = values(name) ?: return emptyList()
        return current.keys.sorted().mapNotNull { id ->
            val wanted = target[id]
            when {
                wanted != null -> if (current[id] == wanted) null else AttributeCommands.setBase(id, wanted)
                current[id] == defaults[id] -> null
                else -> AttributeCommands.resetBase(id)
            }
        }
    }

    /** The values a preset sets, empty for [DEFAULT], or null if there is no such preset. */
    fun values(name: String): Map<String, Double>? = when (name) {
        DEFAULT -> emptyMap()
        in builtIn -> builtIn[name]
        else -> presets[name]
    }

    /** Writes only the player's own presets; built-in ones live in the mod. */
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
        const val BUILT_IN_RESOURCE = "/mc_tiny_tweak/attribute-presets.json"
        private val GSON = GsonBuilder().setPrettyPrinting().create()

        /** The presets bundled with the mod, in the same format as the player's file. */
        fun bundled(): Map<String, Map<String, Double>> =
            AttributePresets::class.java.getResourceAsStream(BUILT_IN_RESOURCE)
                ?.use { parse(it.reader().readText()).presets }
                .orEmpty()

        /** Reads presets, skipping anything that is not a name mapped to numbers. */
        fun parse(text: String, builtIn: Map<String, Map<String, Double>> = emptyMap()): AttributePresets {
            val root = JsonParser.parseString(text).takeIf { it.isJsonObject }?.asJsonObject ?: return AttributePresets()
            val presets = root.entrySet()
                .filter { (name, value) -> !name.trim().equals(DEFAULT, ignoreCase = true) && name.isNotBlank() && value.isJsonObject }
                .associate { (name, value) ->
                    name to value.asJsonObject.entrySet()
                        .mapNotNull { (id, number) ->
                            val v = number.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble
                            v?.takeIf { it.isFinite() }?.let { id to it }
                        }
                        .toMap()
                }
            return AttributePresets(presets, builtIn)
        }
    }
}
