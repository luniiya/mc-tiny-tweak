package dev.luniiya.mctinytweak.healthbar

/**
 * Compact health for more than [LAYER] health points (20 hearts), instead of rows of hearts.
 *
 * Health is split into layers of [LAYER] points, each with its own color from [PALETTE]. Absorption
 * (golden hearts) stacks on top of health in [GOLD], and missing health is [MISSING].
 * - The thin bar shows everything: every layer, then absorption, then what is missing.
 * - The thick bar shows only the top layer, drawn over a full bar of the layer below it.
 */
object HealthBars {
    const val LAYER = 40.0

    /** Layer 1 is orange, then the palette loops from red. Yellow is left out: it means absorption. */
    private const val FIRST = 0xFFFF8C1A.toInt()
    val PALETTE = intArrayOf(
        0xFFE02020.toInt(), // red
        0xFFFF69B4.toInt(), // pink
        0xFFA040E0.toInt(), // purple
        0xFF2838B8.toInt(), // dark blue
        0xFF20C8D8.toInt(), // cyan
        0xFF30C040.toInt(), // green
        0xFFA8E030.toInt(), // yellowish green
    )
    const val GOLD = 0xFFFFC820.toInt()
    const val MISSING = 0xFF000000.toInt()

    /** One colored stretch of a bar, in health points from the bar's start. */
    data class Segment(val start: Double, val end: Double, val color: Int)

    /** Vanilla hearts take a row per 20 points; past two rows the bars take over. */
    fun compact(maxHealth: Float, absorption: Int) = maxHealth + absorption > LAYER

    fun layerColor(layer: Int) = if (layer == 0) FIRST else PALETTE[(layer - 1) % PALETTE.size]

    /** The color of health point [point]: its layer while within health, then gold, then missing. */
    private fun colorAt(point: Double, health: Double, absorption: Double): Int = when {
        point < health -> layerColor((point / LAYER).toInt())
        point < health + absorption -> GOLD
        else -> MISSING
    }

    /** Splits [from, to) into segments wherever the color can change: layer edges, the end of health and absorption. */
    private fun segments(from: Double, to: Double, offset: Double, health: Double, absorption: Double): List<Segment> {
        if (to <= from) return emptyList()
        val edges = sortedSetOf(from, to, health, health + absorption)
        var edge = (Math.floor(from / LAYER) + 1) * LAYER
        while (edge < to) {
            edges += edge
            edge += LAYER
        }
        return edges.filter { it in from..to }.zipWithNext()
            .filter { (a, b) -> b > a }
            .map { (a, b) -> Segment(a - offset, b - offset, colorAt((a + b) / 2, health, absorption)) }
            .fold(mutableListOf()) { merged, s ->
                val last = merged.lastOrNull()
                if (last != null && last.color == s.color && last.end == s.start) merged[merged.size - 1] = last.copy(end = s.end) else merged += s
                merged
            }
    }

    /** The whole picture, from 0 to [maxHealth] + [absorption]. */
    fun thin(health: Double, maxHealth: Double, absorption: Double): List<Segment> {
        val h = health.coerceIn(0.0, maxHealth)
        return segments(0.0, maxHealth + absorption, 0.0, h, absorption)
    }

    /** Where the thin bar's sections between layers are, for the dividers. */
    fun layerEdges(maxHealth: Double, absorption: Double): List<Double> {
        val total = maxHealth + absorption
        return generateSequence(LAYER) { it + LAYER }.takeWhile { it < total }.toList()
    }

    /**
     * The top layer from 0 to [LAYER]: the layer below as a full bar, with the top one over it.
     * With nothing below (the first layer), the rest is missing.
     */
    fun thick(health: Double, maxHealth: Double, absorption: Double): List<Segment> {
        val h = health.coerceIn(0.0, maxHealth)
        val filled = h + absorption
        if (filled <= 0) return listOf(Segment(0.0, LAYER, MISSING))
        val top = Math.ceil(filled / LAYER).toInt() - 1
        val topStart = top * LAYER
        val over = segments(topStart, filled, topStart, h, absorption)
        val under = if (top == 0) listOf(Segment(0.0, LAYER, MISSING))
        else segments(topStart - LAYER, topStart, topStart - LAYER, h, absorption)
        // Draw under, then over on top: the visible result, left to right.
        val coveredTo = filled - topStart
        return over + under.mapNotNull { s -> if (s.end <= coveredTo) null else s.copy(start = maxOf(s.start, coveredTo)) }
    }
}
