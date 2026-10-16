package dev.luniiya.mctinytweak.client.healthbar

import dev.luniiya.mctinytweak.client.tweaks.Tweaks
import dev.luniiya.mctinytweak.healthbar.HealthBars
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.ARGB

/** Draws [HealthBars] where the first row of hearts would be: the thick bar on top, the thin bar under it. */
object CompactHealth {
    private const val WIDTH = 81
    private const val INNER = WIDTH - 2
    private const val THIN_HEIGHT = 2
    private const val THICK_HEIGHT = 6

    @JvmStatic
    fun active(maxHealth: Float, absorption: Int) = Tweaks.compactHealth() && HealthBars.compact(maxHealth, absorption)

    /** [x] and [y] are where vanilla puts the bottom row of hearts. [blink] is the white flash after damage. */
    @JvmStatic
    fun render(graphics: GuiGraphics, x: Int, y: Int, maxHealth: Float, health: Int, absorption: Int, blink: Boolean) {
        val max = maxHealth.toDouble()
        val hp = health.toDouble()
        val gold = absorption.toDouble()

        val border = if (blink) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()

        // Thick bar: the top layer, with a border like the hearts' outline.
        val thickY = y
        graphics.fill(x, thickY, x + WIDTH, thickY + THICK_HEIGHT, border)
        draw(graphics, HealthBars.thick(hp, max, gold), HealthBars.LAYER, x + 1, thickY + 1, THICK_HEIGHT - 2, shade = true, width = INNER)

        // Thin bar: the whole health. Its side borders match the thick bar's so the colors line up;
        // top and bottom only get half a pixel, drawn at half scale, so the bar stays thin.
        val thinY = y + THICK_HEIGHT + 1
        val total = max + gold
        graphics.pose().pushMatrix()
        graphics.pose().scale(0.5f, 0.5f)
        graphics.fill(x * 2, thinY * 2 - 1, (x + WIDTH) * 2, (thinY + THIN_HEIGHT) * 2 + 1, border)
        graphics.pose().popMatrix()
        draw(graphics, HealthBars.thin(hp, max, gold), total, x + 1, thinY, THIN_HEIGHT, shade = false, width = INNER)
        // A dark line between layers when there is room for it.
        val edges = HealthBars.layerEdges(max, gold)
        if (edges.isNotEmpty() && INNER / (edges.size + 1) >= 4) {
            for (edge in edges) {
                val px = x + 1 + (edge / total * INNER).toInt()
                graphics.fill(px, thinY, px + 1, thinY + THIN_HEIGHT, 0x99000000.toInt())
            }
        }
    }

    private fun draw(graphics: GuiGraphics, segments: List<HealthBars.Segment>, length: Double, x: Int, y: Int, height: Int, shade: Boolean, width: Int) {
        if (length <= 0) return
        for (s in segments) {
            val from = x + Math.round(s.start / length * width).toInt()
            val to = x + Math.round(s.end / length * width).toInt()
            if (to <= from) continue
            graphics.fill(from, y, to, y + height, s.color)
            // A lighter top line, like the shine on vanilla hearts.
            if (shade && s.color != HealthBars.MISSING) graphics.fill(from, y, to, y + 1, ARGB.srgbLerp(0.4f, s.color, 0xFFFFFFFF.toInt()))
        }
    }
}
