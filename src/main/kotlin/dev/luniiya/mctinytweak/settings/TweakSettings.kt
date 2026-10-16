package dev.luniiya.mctinytweak.settings

/** Toggles from the Tweaks screen, saved as a small properties file. Unknown or missing keys use the defaults. */
data class TweakSettings(
    /** Off hides player armor and shows [dev.luniiya.mctinytweak.armorbar.ArmorBar] above the name instead. */
    val showArmor: Boolean = false,
) {
    fun serialize() = "$SHOW_ARMOR=$showArmor\n"

    companion object {
        const val FILE_NAME = "mc_tiny_tweak.properties"
        private const val SHOW_ARMOR = "showArmor"

        fun parse(text: String): TweakSettings {
            val values = text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
                .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
            val defaults = TweakSettings()
            return TweakSettings(
                showArmor = values[SHOW_ARMOR]?.toBooleanStrictOrNull() ?: defaults.showArmor,
            )
        }
    }
}
