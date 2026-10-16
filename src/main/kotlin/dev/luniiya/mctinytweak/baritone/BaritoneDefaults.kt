package dev.luniiya.mctinytweak.baritone

/**
 * The mod's own Baritone defaults, kept in `mc_tiny_tweak/baritone-defaults.txt` in Baritone's settings.txt
 * format. They replace Baritone's defaults for the settings they list, both on a new instance and on Reset.
 */
object BaritoneDefaults {
    const val RESOURCE = "/mc_tiny_tweak/baritone-defaults.txt"

    /** Marks an instance that already got the mod's defaults, so later changes are never overwritten. */
    const val APPLIED_MARKER = "mc_tiny_tweak_baritone_defaults_applied"

    /** Setting names, lowercased, mapped to their value text. Blank lines and `#` comments are skipped. */
    fun parse(text: String): Map<String, String> = buildMap {
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val space = line.indexOf(' ')
            if (space <= 0) continue
            put(line.substring(0, space).lowercase(), line.substring(space + 1).trim())
        }
    }

    fun load(): Map<String, String> =
        BaritoneDefaults::class.java.getResourceAsStream(RESOURCE)?.use { parse(it.reader().readText()) } ?: emptyMap()

    /** The default a setting returns to on Reset: the mod's own, or else Baritone's. */
    fun defaultFor(name: String, baritoneDefault: String, modDefaults: Map<String, String>) =
        modDefaults[name.lowercase()] ?: baritoneDefault

    /** Baritone reads anything but `true` as false, so on/off values are checked first. */
    fun isBooleanText(value: String) = value.trim().lowercase() in setOf("true", "false")

    /** Search used by the settings screen and the agent: every word must appear in the name, ignoring case. */
    fun matches(name: String, query: String): Boolean {
        val lower = name.lowercase()
        return query.lowercase().split(' ').filter { it.isNotBlank() }.all { it in lower }
    }
}
