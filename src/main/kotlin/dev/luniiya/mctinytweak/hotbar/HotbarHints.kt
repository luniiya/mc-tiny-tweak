package dev.luniiya.mctinytweak.hotbar

/**
 * Faint shadows drawn in empty slots of the first hotbar group, showing what each slot is for:
 * axe, pickaxe, water bucket, then building blocks.
 */
object HotbarHints {
    enum class Hint(
        /** A vanilla item texture drawn as a silhouette, or null for the [BLOCK] cube drawn from [CUBE]. */
        val texture: String?,
    ) {
        AXE("textures/item/iron_axe.png"),
        PICKAXE("textures/item/iron_pickaxe.png"),
        WATER_BUCKET("textures/item/water_bucket.png"),
        BLOCK(null),
    }

    /** Black at low alpha: multiplied over a texture, it keeps only the outline as a shadow. */
    const val SHADOW_COLOR = 0x55000000

    /** The cube's faces use slightly different shadow strengths so it reads as a block. */
    val FACE_COLORS = mapOf('T' to 0x40000000, 'L' to 0x55000000, 'R' to 0x6A000000)

    /** A 16x16 block icon: T is the top face, L and R the side faces. */
    val CUBE: List<String> = listOf(
        "......TTTT......",
        "....TTTTTTTT....",
        "..TTTTTTTTTTTT..",
        ".TTTTTTTTTTTTTT.",
        ".LTTTTTTTTTTTTR.",
        ".LLLTTTTTTTTRRR.",
        ".LLLLLTTTTRRRRR.",
        ".LLLLLLLRRRRRRR.",
        ".LLLLLLLRRRRRRR.",
        ".LLLLLLLRRRRRRR.",
        ".LLLLLLLRRRRRRR.",
        ".LLLLLLLRRRRRRR.",
        "..LLLLLLRRRRRR..",
        "....LLLLRRRR....",
        "......LLRR......",
        "................",
    )

    /** One horizontal run of the same face, ready to fill: [x0, x1) on row [y]. */
    data class Run(val x0: Int, val x1: Int, val y: Int, val argb: Int)

    fun forSlot(slot: Int): Hint? = Hint.entries.getOrNull(slot)?.takeIf { HotbarLayout.isFirstGroup(slot) }

    /** The cube as fill runs, offset to ([x], [y]). */
    fun cubeRuns(x: Int, y: Int): List<Run> {
        val runs = mutableListOf<Run>()
        for ((row, line) in CUBE.withIndex()) {
            var start = 0
            while (start < line.length) {
                val face = line[start]
                var end = start
                while (end < line.length && line[end] == face) end++
                FACE_COLORS[face]?.let { runs += Run(x + start, x + end, y + row, it) }
                start = end
            }
        }
        return runs
    }
}
