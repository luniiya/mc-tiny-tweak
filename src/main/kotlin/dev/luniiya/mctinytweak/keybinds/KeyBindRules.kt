package dev.luniiya.mctinytweak.keybinds

/** Pure rules behind the Key Binds screen tweaks. Keys are referred to by names such as `key.keyboard.q`. */
object KeyBindRules {
    /** Case-insensitive match of the name search against the bind's displayed name or category. Blank matches all. */
    fun matchesName(query: String, bindName: String, categoryName: String): Boolean {
        val q = query.trim()
        return q.isEmpty() || bindName.contains(q, ignoreCase = true) || categoryName.contains(q, ignoreCase = true)
    }

    /** Whether a bind passes the key search. No key filter matches all binds. */
    fun matchesKey(keyFilter: String?, boundKey: String): Boolean = keyFilter == null || keyFilter == boundKey

    /** Binds that the personal defaults do not list, which "Unbind extras" clears. */
    fun extras(bindNames: Collection<String>, personalDefaults: Map<String, String>): List<String> =
        bindNames.filter { it !in personalDefaults }

    /** How breaking and placing are bound. */
    /** Freelook follows the mode: left click is free in tablet mode, so it takes over freelook there; mouse mode uses middle click. */
    enum class InputMode(val attack: String, val use: String, val freelook: String) {
        TABLET("key.keyboard.q", "key.keyboard.e", "key.mouse.left"),
        MOUSE("key.mouse.left", "key.mouse.right", "key.mouse.middle");

        companion object {
            /** Tablet only when both binds match it exactly; anything else shows as mouse mode. */
            fun detect(attack: String, use: String): InputMode =
                if (attack == TABLET.attack && use == TABLET.use) TABLET else MOUSE
        }
    }
}
