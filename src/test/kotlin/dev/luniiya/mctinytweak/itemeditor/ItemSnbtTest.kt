package dev.luniiya.mctinytweak.itemeditor

import dev.luniiya.mctinytweak.MinecraftTestSupport
import net.minecraft.nbt.TagParser
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ItemSnbtTest {
    private val registries = MinecraftTestSupport.registries

    @Test
    fun `an edited item survives encoding and decoding`() {
        val base = ItemStack(Items.DIAMOND_SWORD)
        val built = (ItemDraft.from(base).copy(
            name = "&c{tricky}, [name]: \"quoted\"",
            lore = listOf("line"),
            enchantments = mapOf("minecraft:sharpness" to 200),
            unbreakable = true,
        ).build(base, registries) as ItemDraft.Result.Success).stack

        val text = ItemSnbt.encode(built, registries)
        assertTrue(text.contains('\n'), "encoded SNBT should be indented")
        val decoded = ItemSnbt.decode(text, registries)
        assertInstanceOf(ItemDraft.Result.Success::class.java, decoded, { "Decoding failed: $decoded\n$text" })
        assertTrue(ItemStack.matches(built, (decoded as ItemDraft.Result.Success).stack))
    }

    @Test
    fun `indent leaves quoted text alone and keeps typed arrays parseable`() {
        val compact = """{a:"x{,}[]:y",b:'it\'s',c:[I;1,2,3],d:[B;],e:{}}"""
        val pretty = ItemSnbt.indent(compact)
        assertEquals(TagParser.parseCompoundFully(compact), TagParser.parseCompoundFully(pretty))
        assertTrue(pretty.contains("\"x{,}[]:y\""))
    }

    @Test
    fun `invalid SNBT and invalid items are reported instead of thrown`() {
        assertInstanceOf(ItemDraft.Result.Failure::class.java, ItemSnbt.decode("{id:", registries))
        assertInstanceOf(ItemDraft.Result.Failure::class.java, ItemSnbt.decode("""{id:"minecraft:nope",count:1}""", registries))
        assertInstanceOf(
            ItemDraft.Result.Failure::class.java,
            ItemSnbt.decode("""{id:"minecraft:diamond_sword",count:5}""", registries),
        )
    }
}
