package dev.luniiya.mctinytweak.hotbar

import dev.luniiya.mctinytweak.mixin.client.hotbar.GuiInvoker
import net.minecraft.client.gui.Gui
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HotbarLayoutTest {
    private val center = 200

    @Test
    fun `the bar stays centered and only grows by the gap and two borders`() {
        assertEquals(HotbarLayout.SPRITE_WIDTH + 2 + HotbarLayout.GAP, HotbarLayout.WIDTH)
        assertEquals(center, (HotbarLayout.left(center) + HotbarLayout.right(center)) / 2)
    }

    @Test
    fun `slots within a group sit 20 pixels apart`() {
        for (slot in 1 until HotbarLayout.SLOTS) {
            if (slot == HotbarLayout.GROUP_SIZE) continue
            assertEquals(20, HotbarLayout.frameX(slot, center) - HotbarLayout.frameX(slot - 1, center), "slot $slot")
        }
    }

    @Test
    fun `the first four slots are set apart by the gap and two borders`() {
        val jump = HotbarLayout.frameX(4, center) - HotbarLayout.frameX(3, center)
        assertEquals(20 + 2 + HotbarLayout.GAP, jump)
        assertTrue((0..3).all(HotbarLayout::isFirstGroup))
        assertTrue((4..8).none(HotbarLayout::isFirstGroup))
    }

    @Test
    fun `items and selection keep vanilla's offsets inside each frame`() {
        for (slot in 0 until HotbarLayout.SLOTS) {
            assertEquals(HotbarLayout.frameX(slot, center) + 2, HotbarLayout.itemX(slot, center))
            assertEquals(HotbarLayout.frameX(slot, center) - 2, HotbarLayout.selectionX(slot, center))
        }
        assertEquals(HotbarLayout.left(center) + 1, HotbarLayout.frameX(0, center))
        assertEquals(HotbarLayout.right(center) - 1, HotbarLayout.frameX(8, center) + 20)
    }

    @Test
    fun `hotbar mixins apply to the vanilla gui`() {
        assertTrue(GuiInvoker::class.java.isAssignableFrom(Gui::class.java))
        assertTrue(Gui::class.java.declaredMethods.any { it.name.contains("renderSplitHotbar") })
    }
}
