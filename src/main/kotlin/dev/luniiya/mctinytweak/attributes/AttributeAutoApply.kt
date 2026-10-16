package dev.luniiya.mctinytweak.attributes

import com.google.gson.GsonBuilder
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive

/**
 * Which attribute preset is applied by itself, and the worlds it already dealt with.
 *
 * A world is dealt with once: when the preset is applied there, or when you load a preset or edit
 * an attribute there yourself. After that it is never applied there again, so loading something
 * else (even Default) sticks across logins.
 */
data class AttributeAutoApply(
    /** The preset to apply, or null when nothing is applied. */
    val preset: String? = DEFAULT_PRESET,
    /** World key to what was last set there: a preset name, or [CUSTOM] for hand edits. */
    val worlds: Map<String, String> = emptyMap(),
) {
    sealed interface Decision {
        data object Nothing : Decision
        /** The values already match this preset, so it is only written down. */
        data class Remember(val preset: String) : Decision
        data class Apply(val preset: String) : Decision
    }

    fun withPreset(name: String?) = copy(preset = name)

    fun remember(world: String, what: String) = copy(worlds = worlds + (world to what))

    /** A deleted preset can no longer be applied. */
    fun deleted(name: String) = if (preset == name) copy(preset = null) else this

    /**
     * What to do on joining [world] with operator rights. [current] and [defaults] hold only the
     * attributes the server sends to the client, since the others always look like their default.
     */
    fun decide(world: String, current: Map<String, Double>, defaults: Map<String, Double>, presets: AttributePresets): Decision {
        val auto = preset?.takeIf { presets.values(it) != null } ?: return Decision.Nothing
        if (world in worlds) return Decision.Nothing
        // Every value at its default is a fresh player, not someone who picked Default.
        val fresh = current.all { (id, value) -> value == defaults[id] }
        if (!fresh) {
            val candidates = listOf(auto) + presets.names.filter { it != auto && it != AttributePresets.DEFAULT }
            candidates.firstOrNull { matches(presets.values(it).orEmpty(), current, defaults) }?.let { return Decision.Remember(it) }
        }
        return Decision.Apply(auto)
    }

    fun serialize(): String {
        val root = JsonObject()
        root.add("preset", preset?.let(::JsonPrimitive) ?: JsonNull.INSTANCE)
        root.add("worlds", JsonObject().apply { worlds.toSortedMap().forEach { (k, v) -> addProperty(k, v) } })
        return GSON.toJson(root) + "\n"
    }

    companion object {
        const val DEFAULT_PRESET = "boosted"
        const val CUSTOM = ""
        const val FILE_NAME = "mc_tiny_tweak_attribute_auto_apply.json"
        private val GSON = GsonBuilder().setPrettyPrinting().serializeNulls().create()

        private fun matches(values: Map<String, Double>, current: Map<String, Double>, defaults: Map<String, Double>) =
            current.all { (id, value) -> value == (values[id] ?: defaults[id]) }

        /** A missing `preset` means the default one; an explicit null means none. */
        fun parse(text: String): AttributeAutoApply {
            val root = runCatching { JsonParser.parseString(text) }.getOrNull()?.takeIf { it.isJsonObject }?.asJsonObject ?: return AttributeAutoApply()
            val preset = when {
                !root.has("preset") -> DEFAULT_PRESET
                root.get("preset").isJsonPrimitive -> root.get("preset").asString
                else -> null
            }
            val worlds = root.get("worlds")?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()
                ?.filter { it.value.isJsonPrimitive }
                ?.associate { it.key to it.value.asString }
                .orEmpty()
            return AttributeAutoApply(preset, worlds)
        }
    }
}
