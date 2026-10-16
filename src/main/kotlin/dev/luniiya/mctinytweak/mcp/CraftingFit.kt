package dev.luniiya.mctinytweak.mcp

/** Whether a crafting recipe fits a grid: 2x2 in the inventory, 3x3 at a crafting table. */
object CraftingFit {
    fun shaped(width: Int, height: Int, gridWidth: Int, gridHeight: Int) = width <= gridWidth && height <= gridHeight

    fun shapeless(ingredients: Int, gridWidth: Int, gridHeight: Int) = ingredients <= gridWidth * gridHeight

    /** Accepts `stick` or `minecraft:stick`. */
    fun itemId(text: String): String = text.trim().lowercase().let { if (':' in it) it else "minecraft:$it" }
}
