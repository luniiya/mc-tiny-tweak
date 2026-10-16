package dev.luniiya.mctinytweak.mcp

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CraftingFitTest {
    @Test
    fun `the inventory grid fits 2x2 recipes and a crafting table fits 3x3`() {
        assertTrue(CraftingFit.shaped(2, 2, 2, 2), "crafting table recipe")
        assertTrue(CraftingFit.shaped(1, 2, 2, 2), "sticks")
        assertFalse(CraftingFit.shaped(3, 3, 2, 2), "pickaxe needs a table")
        assertTrue(CraftingFit.shaped(3, 3, 3, 3))
        assertTrue(CraftingFit.shapeless(1, 2, 2), "planks")
        assertFalse(CraftingFit.shapeless(5, 2, 2))
    }

    @Test
    fun `item names get the minecraft namespace`() {
        assertEquals("minecraft:stick", CraftingFit.itemId(" Stick "))
        assertEquals("other:thing", CraftingFit.itemId("other:thing"))
    }
}
