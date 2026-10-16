package dev.luniiya.mctinytweak.itemeditor

/** Pure helpers for the enchantment picker. */
object EnchantmentLevels {
    /** Returns [levels] with [id] set to [level], keeping existing order; a level of 0 or less removes it. */
    fun with(levels: Map<String, Int>, id: String, level: Int): Map<String, Int> {
        val result = LinkedHashMap(levels)
        if (level <= 0) result.remove(id) else result[id] = level
        return result
    }

    /** Case-insensitive match against the id or the displayed name. Blank queries match everything. */
    fun matches(query: String, id: String, name: String): Boolean {
        val q = query.trim()
        return q.isEmpty() || id.contains(q, ignoreCase = true) || name.contains(q, ignoreCase = true)
    }

    /**
     * Enchantments that fit the item come first, then the rest, each group in name order.
     * The others still work when applied; they are just not what the item normally takes.
     */
    fun <T> compatibleFirst(entries: List<T>, compatible: (T) -> Boolean, name: (T) -> String): List<T> =
        entries.sortedWith(compareBy<T>({ !compatible(it) }, { name(it).lowercase() }))
}
