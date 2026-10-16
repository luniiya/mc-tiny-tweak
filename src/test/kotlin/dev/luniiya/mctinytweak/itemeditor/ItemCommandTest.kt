package dev.luniiya.mctinytweak.itemeditor

import com.mojang.brigadier.StringReader
import dev.luniiya.mctinytweak.MinecraftTestSupport
import net.minecraft.commands.arguments.item.ItemInput
import net.minecraft.commands.arguments.item.ItemParser
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ItemCommandTest {
    private val registries = MinecraftTestSupport.registries

    /** Parses the item argument back the way the server's `/item` command does. */
    private fun parseBack(command: String, prefix: String): ItemStack {
        assertTrue(command.startsWith(prefix), command)
        val reader = StringReader(command.removePrefix(prefix))
        val parsed = ItemParser(registries).parse(reader)
        reader.expect(' ')
        return ItemInput(parsed.item(), parsed.components()).createItemStack(reader.readInt(), true)
    }

    @Test
    fun `an edited item survives the operator command`() {
        val base = ItemStack(Items.DIAMOND_SWORD)
        val built = (ItemDraft.from(base).copy(
            name = "&c{tricky}, [name]: \"quoted\" 'single'",
            lore = listOf("&#FF8800line", "two"),
            enchantments = mapOf("minecraft:sharpness" to 200),
            unbreakable = true,
            damage = 12,
        ).build(base, registries) as ItemDraft.Result.Success).stack

        val command = ItemCommand.replaceHotbar(built, 4, registries)
        assertTrue(ItemStack.matches(built, parseBack(command, "item replace entity @s hotbar.4 with ")), command)
    }

    @Test
    fun `stacks above the normal size and removed components survive the command`() {
        val base = ItemStack(Items.APPLE)
        val built = (ItemDraft.from(base).copy(count = 99).build(base, registries) as ItemDraft.Result.Success).stack
        built.remove(DataComponents.FOOD)

        val command = ItemCommand.replaceHotbar(built, 0, registries)
        assertTrue(command.contains("!minecraft:food"), command)
        assertTrue(ItemStack.matches(built, parseBack(command, "item replace entity @s hotbar.0 with ")), command)
    }

    @Test
    fun `slots outside the hotbar are rejected`() {
        assertThrows<IllegalArgumentException> { ItemCommand.replaceHotbar(ItemStack(Items.STONE), 9, registries) }
    }
}
