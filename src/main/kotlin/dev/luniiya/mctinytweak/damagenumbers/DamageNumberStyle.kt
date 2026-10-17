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
    /** Numbers use the mod's chunky font instead of Minecraft's. */
    val font: Boolean = true,
    /** 100+ damage sends shockwave rings out from the number. */
    val rings: Boolean = true,
    /** 100+ damage slashes across the mob. */
    val slash: Boolean = true,
    /** 200+ damage leaves afterimages trailing the number. */
    val afterimages: Boolean = true,
    /** 500+ damage spins light rays behind the number. */
    val rays: Boolean = true,
    /** 500+ damage shatters the number at the end. */
    val shatter: Boolean = true,
    /** 500+ damage draws anime focus lines around the screen. */
    val focusLines: Boolean = true,
    /** 990+ damage cycles every heart color across the digits and adds a MAX tag and a starburst. */
    val rainbow: Boolean = true,
    /** 990+ damage inverts the world for an instant, then flashes the screen white. */
    val impactFrame: Boolean = true,
    /** 100+ damage plays a heavier sound for each tier. Only you hear it. */
    val sounds: Boolean = true,
    /** Only hits the player made get numbers. Off shows everyone's hits. */
    val onlyMine: Boolean = false,
    /** Mobs healing show a green +number. */
    val healNumbers: Boolean = true,
    /** A gacha-style "24 HITS" counter for the player's hits in a row. */
    val comboCounter: Boolean = true,
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
            Key("font", { it.font }) { s, v -> s.copy(font = v) },
            Key("rings", { it.rings }) { s, v -> s.copy(rings = v) },
            Key("slash", { it.slash }) { s, v -> s.copy(slash = v) },
            Key("afterimages", { it.afterimages }) { s, v -> s.copy(afterimages = v) },
            Key("rays", { it.rays }) { s, v -> s.copy(rays = v) },
            Key("shatter", { it.shatter }) { s, v -> s.copy(shatter = v) },
            Key("focusLines", { it.focusLines }) { s, v -> s.copy(focusLines = v) },
            Key("rainbow", { it.rainbow }) { s, v -> s.copy(rainbow = v) },
            Key("impactFrame", { it.impactFrame }) { s, v -> s.copy(impactFrame = v) },
            Key("sounds", { it.sounds }) { s, v -> s.copy(sounds = v) },
            Key("onlyMine", { it.onlyMine }) { s, v -> s.copy(onlyMine = v) },
            Key("healNumbers", { it.healNumbers }) { s, v -> s.copy(healNumbers = v) },
            Key("comboCounter", { it.comboCounter }) { s, v -> s.copy(comboCounter = v) },
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
