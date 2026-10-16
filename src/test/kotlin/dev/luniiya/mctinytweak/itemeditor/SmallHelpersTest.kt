package dev.luniiya.mctinytweak.itemeditor

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SmallHelpersTest {
    @Test
    fun `lore split keeps inner blank lines and drops trailing ones`() {
        assertEquals(listOf("a", "", "b"), LoreLines.split("a\n\nb\n\n"))
        assertEquals(emptyList<String>(), LoreLines.split(""))
        assertEquals("a\n\nb", LoreLines.join(listOf("a", "", "b")))
    }

    @Test
    fun `enchantment levels set, replace in place and remove at zero`() {
        var levels = EnchantmentLevels.with(emptyMap(), "minecraft:sharpness", 5)
        levels = EnchantmentLevels.with(levels, "minecraft:looting", 3)
        levels = EnchantmentLevels.with(levels, "minecraft:sharpness", 10)
        assertEquals(listOf("minecraft:sharpness" to 10, "minecraft:looting" to 3), levels.toList())
        assertEquals(mapOf("minecraft:looting" to 3), EnchantmentLevels.with(levels, "minecraft:sharpness", 0))
    }

    @Test
    fun `compatible enchantments come first, each group in name order`() {
        val entries = listOf("Thorns" to false, "Sharpness" to true, "Aqua Affinity" to false, "Looting" to true, "breach" to false)
        val ordered = EnchantmentLevels.compatibleFirst(entries, { it.second }, { it.first }).map { it.first }
        assertEquals(listOf("Looting", "Sharpness", "Aqua Affinity", "breach", "Thorns"), ordered)
    }

    @Test
    fun `enchantment search matches id or name ignoring case`() {
        assertTrue(EnchantmentLevels.matches("", "minecraft:sharpness", "Sharpness"))
        assertTrue(EnchantmentLevels.matches("SHARP", "minecraft:sharpness", "Sharpness"))
        assertTrue(EnchantmentLevels.matches("tranchant", "minecraft:sharpness", "Tranchant"))
        assertFalse(EnchantmentLevels.matches("loot", "minecraft:sharpness", "Sharpness"))
    }
}
