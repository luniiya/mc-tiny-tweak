package dev.luniiya.mctinytweak.damagenumbers

/** Which gacha-style effects the damage numbers use, saved as a small properties file. All on by default. */
data class DamageNumberStyle(
    /** Big hits spawn much bigger, overshoot and bounce back. Off keeps a small pop. */
    val slam: Boolean = true,
    /** Big hits and crits start white-hot and fade into their color. */
    val flash: Boolean = true,
    /** Big hits and crits shake for a moment when they land. */
    val shake: Boolean = true,
    /** Big hits and crits burst sparks around the number. */
    val burst: Boolean = true,
    /** Huge hits and crits flow their heart color, the next one and gold across their digits. */
    val gradient: Boolean = true,
    /** Quick hits on the same entity add up into one growing number. */
    val combo: Boolean = true,
    /** Numbers count up to their value as they appear. */
    val countUp: Boolean = true,
    /** Crits get a ✦ after the number. */
    val critMark: Boolean = true,
) {
    fun serialize() = KEYS.joinToString("") { (key, get, _) -> "$key=${get(this)}\n" }

    companion object {
        const val FILE_NAME = "mc_tiny_tweak_damage_numbers.properties"

        private data class Key(
            val name: String,
            val get: (DamageNumberStyle) -> Boolean,
            val set: (DamageNumberStyle, Boolean) -> DamageNumberStyle,
        )

        private val KEYS = listOf(
            Key("slam", { it.slam }) { s, v -> s.copy(slam = v) },
            Key("flash", { it.flash }) { s, v -> s.copy(flash = v) },
            Key("shake", { it.shake }) { s, v -> s.copy(shake = v) },
            Key("burst", { it.burst }) { s, v -> s.copy(burst = v) },
            Key("gradient", { it.gradient }) { s, v -> s.copy(gradient = v) },
            Key("combo", { it.combo }) { s, v -> s.copy(combo = v) },
            Key("countUp", { it.countUp }) { s, v -> s.copy(countUp = v) },
            Key("critMark", { it.critMark }) { s, v -> s.copy(critMark = v) },
        )

        fun parse(text: String): DamageNumberStyle {
            val values = text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
                .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
            return KEYS.fold(DamageNumberStyle()) { style, key ->
                values[key.name]?.toBooleanStrictOrNull()?.let { key.set(style, it) } ?: style
            }
        }
    }
}
