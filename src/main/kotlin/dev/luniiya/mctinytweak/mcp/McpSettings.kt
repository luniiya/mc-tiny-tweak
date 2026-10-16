package dev.luniiya.mctinytweak.mcp

/**
 * Settings for the MCP server that lets an agent play the game, saved per instance as a properties file.
 * Everything is on by default; unknown or missing keys use the defaults.
 */
data class McpSettings(
    val enabled: Boolean = true,
    /**
     * The first port tried. When it is taken, for example by another game, the next free one is used and remembered
     * here, so each instance keeps its own port across restarts whatever order the games start in.
     */
    val port: Int = DEFAULT_PORT,
    val features: Set<Feature> = Feature.entries.toSet(),
    /** Echo every tool call in chat as a mod notice, so the player can follow along. */
    val showActions: Boolean = true,
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
        TAKE_OVER("takeOver"),
        WORLDS("worlds"),
    }

    fun allows(feature: Feature) = feature in features

    fun with(feature: Feature, on: Boolean) = copy(features = if (on) features + feature else features - feature)

    /** These settings with [bound] remembered as this instance's port, or the same settings when it already is. */
    fun rememberingPort(bound: Int) = if (bound == port) this else copy(port = bound)

    fun serialize() = buildString {
        append("$ENABLED=$enabled\n")
        append("$PORT=$port\n")
        append("$SHOW_ACTIONS=$showActions\n")
        for (feature in Feature.entries) append("$FEATURE_PREFIX${feature.key}=${feature in features}\n")
    }

    companion object {
        const val FILE_NAME = "mc_tiny_tweak_mcp.properties"
        const val DEFAULT_PORT = 25599

        /** How many ports in a row are tried, enough for a lot of games open at once. */
        const val PORT_ATTEMPTS = 100

        /**
         * The MCP server name an agent knows this game by. The first game keeps the plain name; the others add their
         * port, so adding a second game to an agent does not replace the first.
         */
        fun serverName(port: Int) = if (port == DEFAULT_PORT) "minecraft" else "minecraft-$port"
        private const val ENABLED = "enabled"
        private const val PORT = "port"
        private const val SHOW_ACTIONS = "showActions"
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
                showActions = values[SHOW_ACTIONS]?.toBooleanStrictOrNull() ?: defaults.showActions,
            )
        }
    }
}
