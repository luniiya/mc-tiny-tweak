package dev.luniiya.mctinytweak.settings

/** Toggles from the Tweaks screen, saved as a small properties file. Unknown or missing keys use the defaults. */
data class TweakSettings(
    /** Off hides player armor and shows [dev.luniiya.mctinytweak.armorbar.ArmorBar] above the name instead. */
    val showArmor: Boolean = false,
    /** Lights everything as if the player had night vision. */
    val fullbright: Boolean = false,
    /** Past 20 hearts, shows [dev.luniiya.mctinytweak.healthbar.HealthBars] instead of rows of hearts. */
    val compactHealth: Boolean = true,
) {
    fun serialize() = "$SHOW_ARMOR=$showArmor\n$FULLBRIGHT=$fullbright\n$COMPACT_HEALTH=$compactHealth\n"

    companion object {
        const val FILE_NAME = "mc_tiny_tweak.properties"
        private const val SHOW_ARMOR = "showArmor"
        private const val FULLBRIGHT = "fullbright"
        private const val COMPACT_HEALTH = "compactHealth"

        fun parse(text: String): TweakSettings {
            val values = text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
                .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
            val defaults = TweakSettings()
            return TweakSettings(
                showArmor = values[SHOW_ARMOR]?.toBooleanStrictOrNull() ?: defaults.showArmor,
                fullbright = values[FULLBRIGHT]?.toBooleanStrictOrNull() ?: defaults.fullbright,
                compactHealth = values[COMPACT_HEALTH]?.toBooleanStrictOrNull() ?: defaults.compactHealth,
            )
        }
    }
}
