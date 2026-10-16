package dev.luniiya.mctinytweak.itemeditor

import dev.luniiya.mctinytweak.MinecraftTestSupport
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ItemDraftTest {
    private val registries = MinecraftTestSupport.registries
    private val sharpness get() = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS)

    private fun success(result: ItemDraft.Result): ItemStack {
        assertInstanceOf(ItemDraft.Result.Success::class.java, result, { "Expected success but got $result" })
        return (result as ItemDraft.Result.Success).stack
    }

    private fun errors(result: ItemDraft.Result): List<String> {
        assertInstanceOf(ItemDraft.Result.Failure::class.java, result, { "Expected failure but got $result" })
        return (result as ItemDraft.Result.Failure).errors
    }

    @Test
    fun `every edited field is written and read back`() {
        val base = ItemStack(Items.DIAMOND_SWORD)
        val draft = ItemDraft.from(base).copy(
            damage = 5,
            unbreakable = true,
            name = "&cBlade",
            lore = listOf("&7first", "", "second"),
            enchantments = mapOf("minecraft:sharpness" to 10),
        )
        val built = success(draft.build(base, registries))

        assertEquals(5, built.damageValue)
        assertTrue(built.has(DataComponents.UNBREAKABLE))
        assertEquals("Blade", built.get(DataComponents.CUSTOM_NAME)!!.string)
        assertEquals(listOf("first", "", "second"), built.get(DataComponents.LORE)!!.lines().map { it.string })
        assertEquals(10, built.get(DataComponents.ENCHANTMENTS)!!.getLevel(sharpness))
        assertEquals(draft, ItemDraft.from(built))
    }

    @Test
    fun `unedited components on the base item are kept`() {
        val base = ItemStack(Items.LEATHER_HELMET)
        base.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
        val built = success(ItemDraft.from(base).copy(name = "Hat").build(base, registries))
        assertEquals(true, built.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
    }

    @Test
    fun `changing the item id keeps the custom name`() {
        val base = ItemStack(Items.STONE)
        val named = success(ItemDraft.from(base).copy(name = "Rock").build(base, registries))
        val changed = success(ItemDraft.from(named).copy(itemId = "minecraft:diamond").build(named, registries))
        assertTrue(changed.`is`(Items.DIAMOND))
        assertEquals("Rock", changed.get(DataComponents.CUSTOM_NAME)!!.string)
    }

    @Test
    fun `clearing name lore and enchantments returns the item to its defaults`() {
        val base = ItemStack(Items.IRON_SWORD)
        val filled = success(
            ItemDraft.from(base).copy(name = "x", lore = listOf("y"), enchantments = mapOf("minecraft:sharpness" to 1)).build(base, registries),
        )
        val cleared = success(ItemDraft.from(filled).copy(name = " ", lore = emptyList(), enchantments = emptyMap()).build(filled, registries))
        assertFalse(cleared.has(DataComponents.CUSTOM_NAME))
        assertTrue(cleared.get(DataComponents.LORE)!!.lines().isEmpty())
        assertTrue(cleared.get(DataComponents.ENCHANTMENTS)!!.isEmpty)
        assertTrue(cleared.componentsPatch.isEmpty, cleared.componentsPatch.toString())
    }

    @Test
    fun `unbreakable items keep their damage value`() {
        // Regression: isDamageableItem is false for unbreakable items, which made damage read back as 0.
        val base = ItemStack(Items.DIAMOND_SWORD)
        val built = success(ItemDraft.from(base).copy(damage = 40, unbreakable = true).build(base, registries))
        assertEquals(40, ItemDraft.from(built).damage)
        val edited = success(ItemDraft.from(built).copy(damage = 41).build(built, registries))
        assertEquals(41, edited.damageValue)
    }

    @Test
    fun `stacks above the normal limit get a max stack size so the server accepts them`() {
        val base = ItemStack(Items.ENDER_PEARL)
        val built = success(ItemDraft.from(base).copy(count = 64).build(base, registries))
        assertEquals(64, built.count)
        assertEquals(64, built.maxStackSize)
        assertTrue(ItemStack.validateStrict(built).isSuccess)
    }

    @Test
    fun `damageable items cannot stack`() {
        val base = ItemStack(Items.DIAMOND_SWORD)
        assertEquals(listOf("Damageable items cannot stack above 1"), errors(ItemDraft.from(base).copy(count = 2).build(base, registries)))
    }

    @Test
    fun `invalid values are reported`() {
        val base = ItemStack(Items.DIAMOND_SWORD)
        val draft = ItemDraft.from(base)
        assertEquals(listOf("Unknown item: minecraft:nope"), errors(draft.copy(itemId = "minecraft:nope").build(base, registries)))
        assertEquals(listOf("Unknown item: minecraft:air"), errors(draft.copy(itemId = "minecraft:air").build(base, registries)))
        assertEquals(listOf("Count must be between 1 and 99"), errors(draft.copy(count = 0).build(base, registries)))
        assertEquals(listOf("Damage must be between 0 and 1561"), errors(draft.copy(damage = 5000).build(base, registries)))
        assertEquals(listOf("Unknown enchantment: minecraft:nope"), errors(draft.copy(enchantments = mapOf("minecraft:nope" to 1)).build(base, registries)))
        assertEquals(
            listOf("Level of minecraft:sharpness must be between 1 and 255"),
            errors(draft.copy(enchantments = mapOf("minecraft:sharpness" to 256)).build(base, registries)),
        )
        val stone = ItemStack(Items.STONE)
        assertEquals(listOf("This item cannot take damage"), errors(ItemDraft.from(stone).copy(damage = 3).build(stone, registries)))
    }

    @Test
    fun `enchanted books store their enchantments separately`() {
        val base = ItemStack(Items.ENCHANTED_BOOK)
        val built = success(ItemDraft.from(base).copy(enchantments = mapOf("minecraft:sharpness" to 7)).build(base, registries))
        assertEquals(7, built.get(DataComponents.STORED_ENCHANTMENTS)!!.getLevel(sharpness))
        assertEquals(mapOf("minecraft:sharpness" to 7), ItemDraft.from(built).enchantments)
    }
}
