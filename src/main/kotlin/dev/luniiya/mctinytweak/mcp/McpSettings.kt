package dev.luniiya.mctinytweak.mcp

/**
 * Settings for the MCP server that lets an agent play the game, saved per instance as a properties file.
 * Everything is on by default; unknown or missing keys use the defaults.
 */
data class McpSettings(
    val enabled: Boolean = true,
    /** The first port tried. When it is taken, for example by a second game, the next free one is used. */
    val port: Int = DEFAULT_PORT,
    val features: Set<Feature> = Feature.entries.toSet(),
) {
    /** What the agent may do. Each tool belongs to one feature and disappears when it is off. */
    enum class Feature(val key: String) {
        STATE("state"),
        SCREENSHOT("screenshot"),
        CHAT_READ("chatRead"),
        CHAT_SEND("chatSend"),
        COMMANDS("commands"),
        INPUT("input"),
        BARITONE("baritone"),
    }

    fun allows(feature: Feature) = feature in features

    fun with(feature: Feature, on: Boolean) = copy(features = if (on) features + feature else features - feature)

    fun serialize() = buildString {
        append("$ENABLED=$enabled\n")
        append("$PORT=$port\n")
        for (feature in Feature.entries) append("$FEATURE_PREFIX${feature.key}=${feature in features}\n")
    }

    companion object {
        const val FILE_NAME = "mc_tiny_tweak_mcp.properties"
        const val DEFAULT_PORT = 25599
        private const val ENABLED = "enabled"
        private const val PORT = "port"
        private const val FEATURE_PREFIX = "feature."

        fun parse(text: String): McpSettings {
            val values = text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
                .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
            val defaults = McpSettings()
            return McpSettings(
                enabled = values[ENABLED]?.toBooleanStrictOrNull() ?: defaults.enabled,
                port = values[PORT]?.toIntOrNull()?.takeIf { it in 1024..65535 } ?: defaults.port,
                features = Feature.entries.filter {
                    values[FEATURE_PREFIX + it.key]?.toBooleanStrictOrNull() ?: defaults.allows(it)
                }.toSet(),
            )
        }
    }
}
