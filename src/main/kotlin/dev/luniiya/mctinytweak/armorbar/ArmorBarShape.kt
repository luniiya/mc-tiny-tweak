package dev.luniiya.mctinytweak.armorbar

/**
 * Lays out the armor bar as flat rectangles in name tag pixels, styled after a gray experience bar: a dark
 * outline, dark notches between the four segments, and fills with a lighter top row and a darker bottom row.
 * Rectangles never overlap, so they can share one depth without flickering.
 */
object ArmorBarShape {
    const val SEGMENT_WIDTH = 9
    const val HEIGHT = 5
    const val WIDTH = 2 + ArmorBar.SEGMENTS * SEGMENT_WIDTH + (ArmorBar.SEGMENTS - 1)
    /** Gap between the bar and the top of the name tag background. */
    const val GAP_ABOVE_NAME = 2

    const val OUTLINE = 0xFF1C1C1C.toInt()
    /** The empty track, a desaturated version of the experience bar background. */
    const val TRACK_TOP = 0xFF545454.toInt()
    const val TRACK = 0xFF3A3A3A.toInt()
    const val TRACK_BOTTOM = 0xFF2E2E2E.toInt()

    data class Rect(val x0: Int, val y0: Int, val x1: Int, val y1: Int, val argb: Int)

    /** The bar's top edge, in the same units as the name tag's vertical offset (negative is up). */
    fun top(nameOffset: Int) = nameOffset - 1 - GAP_ABOVE_NAME - HEIGHT

    /** Rectangles for [segments] (RGB colors, [ArmorBar.EMPTY_COLOR] for empty), centered on x = 0. */
    fun rects(segments: List<Int>, top: Int): List<Rect> {
        val left = -WIDTH / 2
        val right = left + WIDTH
        val bottom = top + HEIGHT
        val rects = mutableListOf(
            Rect(left, top, right, top + 1, OUTLINE),
            Rect(left, bottom - 1, right, bottom, OUTLINE),
            Rect(left, top + 1, left + 1, bottom - 1, OUTLINE),
            Rect(right - 1, top + 1, right, bottom - 1, OUTLINE),
        )
        var x = left + 1
        segments.forEachIndexed { index, rgb ->
            val (high, mid, low) = rows(rgb)
            rects += Rect(x, top + 1, x + SEGMENT_WIDTH, top + 2, high)
            rects += Rect(x, top + 2, x + SEGMENT_WIDTH, top + 3, mid)
            rects += Rect(x, top + 3, x + SEGMENT_WIDTH, top + 4, low)
            x += SEGMENT_WIDTH
            if (index < segments.lastIndex) {
                rects += Rect(x, top + 1, x + 1, bottom - 1, OUTLINE)
                x += 1
            }
        }
        return rects
    }

    /** Top, middle and bottom row colors of one segment. */
    fun rows(rgb: Int): Triple<Int, Int, Int> =
        if (rgb == ArmorBar.EMPTY_COLOR) Triple(TRACK_TOP, TRACK, TRACK_BOTTOM)
        else Triple(opaque(mix(rgb, 0xFFFFFF, 0.35f)), opaque(rgb), opaque(mix(rgb, 0x000000, 0.3f)))

    private fun opaque(rgb: Int) = rgb or 0xFF000000.toInt()

    private fun mix(a: Int, b: Int, t: Float): Int {
        fun channel(shift: Int): Int {
            val x = (a shr shift) and 0xFF
            val y = (b shr shift) and 0xFF
            return (x + (y - x) * t).toInt().coerceIn(0, 255) shl shift
        }
        return channel(16) or channel(8) or channel(0)
    }
}
