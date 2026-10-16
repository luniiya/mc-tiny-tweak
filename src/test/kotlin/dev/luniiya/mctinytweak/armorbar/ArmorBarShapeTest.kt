package dev.luniiya.mctinytweak.armorbar

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ArmorBarShapeTest {
    private val empty = List(ArmorBar.SEGMENTS) { ArmorBar.EMPTY_COLOR }
    private val top = ArmorBarShape.top(0)

    private fun cells(rects: List<ArmorBarShape.Rect>): List<Pair<Int, Int>> =
        rects.flatMap { r -> (r.x0 until r.x1).flatMap { x -> (r.y0 until r.y1).map { y -> x to y } } }

    @Test
    fun `the bar is thin, centered and sits just above the name`() {
        assertEquals(5, ArmorBarShape.HEIGHT)
        val rects = ArmorBarShape.rects(empty, top)
        assertEquals(-ArmorBarShape.WIDTH / 2, rects.minOf { it.x0 })
        assertEquals(ArmorBarShape.WIDTH, rects.maxOf { it.x1 } - rects.minOf { it.x0 })
        // The name tag background starts one pixel above the text, at offset -1.
        assertTrue(rects.maxOf { it.y1 } < -1, "bar must not touch the name tag background")
    }

    @Test
    fun `rectangles cover the whole bar exactly once`() {
        val covered = cells(ArmorBarShape.rects(listOf(0x4AEDD9, 0xE6E6E6, ArmorBar.EMPTY_COLOR, ArmorBar.EMPTY_COLOR), top))
        assertEquals(covered.size, covered.toSet().size, "overlapping rectangles would flicker")
        assertEquals(ArmorBarShape.WIDTH * ArmorBarShape.HEIGHT, covered.size)
    }

    @Test
    fun `one piece fills a quarter of the track in its color with experience bar shading`() {
        val netherite = 0x5E5458
        val rects = ArmorBarShape.rects(listOf(netherite) + empty.drop(1), top)
        val fill = rects.filter { it.argb != ArmorBarShape.OUTLINE && it.argb !in listOf(ArmorBarShape.TRACK_TOP, ArmorBarShape.TRACK, ArmorBarShape.TRACK_BOTTOM) }
        assertEquals(3, fill.size, "one segment, three shaded rows")
        assertEquals(ArmorBarShape.SEGMENT_WIDTH, fill.first().x1 - fill.first().x0)
        assertTrue(fill.all { it.x0 == -ArmorBarShape.WIDTH / 2 + 1 }, "fills from the left")
        val (high, mid, low) = ArmorBarShape.rows(netherite)
        assertEquals(netherite or 0xFF000000.toInt(), mid)
        assertTrue(brightness(high) > brightness(mid) && brightness(mid) > brightness(low))
    }

    @Test
    fun `the empty track is gray, not experience green`() {
        val (high, mid, low) = ArmorBarShape.rows(ArmorBar.EMPTY_COLOR)
        for (argb in listOf(high, mid, low)) {
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            assertTrue(r == g && g == b, "track color %08X is not gray".format(argb))
        }
    }

    private fun brightness(argb: Int) = ((argb shr 16) and 0xFF) + ((argb shr 8) and 0xFF) + (argb and 0xFF)
}
