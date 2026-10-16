package dev.luniiya.mctinytweak.baritone

/**
 * The mod's own Baritone defaults, kept in `mc_tiny_tweak/baritone-defaults.txt` in Baritone's settings.txt
 * format. They replace Baritone's defaults for the settings they list, both on a new instance and on Reset.
 */
object BaritoneDefaults {
    const val RESOURCE = "/mc_tiny_tweak/baritone-defaults.txt"

    /** Lists the defaults an instance already got, one name per line, so the player's later changes are kept. */
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

    /** The defaults this instance has not had yet: all of them at first, and later only newly added ones. */
    fun notYetApplied(defaults: Map<String, String>, markerText: String?): Map<String, String> {
        val applied = markerText?.lineSequence()?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() && !it.startsWith("#") }?.toSet() ?: emptySet()
        return defaults.filterKeys { it !in applied }
    }

    fun markerText(applied: Collection<String>) =
        "# Baritone defaults from MC Tiny Tweak already applied here. Remove a line to apply it again.\n" +
            applied.sorted().joinToString("") { "$it\n" }

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
