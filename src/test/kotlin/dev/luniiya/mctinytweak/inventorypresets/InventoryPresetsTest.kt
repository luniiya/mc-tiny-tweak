package dev.luniiya.mctinytweak.inventorypresets

import com.mojang.brigadier.StringReader
import dev.luniiya.mctinytweak.MinecraftTestSupport
import dev.luniiya.mctinytweak.itemeditor.ItemCommand
import net.minecraft.commands.arguments.item.ItemParser
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.world.inventory.SlotRanges
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class InventoryPresetsTest {
    private val registries = MinecraftTestSupport.registries

    private fun sword(): ItemStack = ItemStack(Items.DIAMOND_SWORD).also {
        val sharpness = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS)
        it.enchant(sharpness, 5)
    }

    @Test
    fun `a preset covers hotbar, inventory, armor and offhand with real item slot names`() {
        assertEquals(41, InventoryPresets.SLOT_NAMES.size)
        for (name in InventoryPresets.SLOT_NAMES) assertNotNull(SlotRanges.nameToIds(name), name)
    }

    @Test
    fun `presets with enchanted items survive the file`() {
        val slots = mapOf("hotbar.0" to sword(), "armor.head" to ItemStack(Items.DIAMOND_HELMET), "inventory.26" to ItemStack(Items.OAK_PLANKS, 64))
        val presets = InventoryPresets().with(3, slots)
        val out = ByteArrayOutputStream()
        NbtIo.writeCompressed(presets.encode(registries), out)
        val back = InventoryPresets.decode(NbtIo.readCompressed(ByteArrayInputStream(out.toByteArray()), NbtAccounter.unlimitedHeap()), registries)
        assertTrue(InventoryPresets.sameSlots(slots, back[3]!!))
        assertNull(back[0])
    }

    @Test
    fun `saving an empty inventory clears the preset`() {
        val presets = InventoryPresets().with(0, mapOf("hotbar.0" to sword())).with(0, emptyMap())
        assertNull(presets[0])
    }

    @Test
    fun `the icon is the first hotbar item`() {
        val presets = InventoryPresets().with(1, mapOf("armor.head" to ItemStack(Items.IRON_HELMET), "hotbar.4" to ItemStack(Items.BOW)))
        assertEquals(Items.BOW, presets.icon(1).item)
        assertTrue(presets.icon(2).isEmpty)
    }

    @Test
    fun `loading only touches slots that differ and clears ones the preset leaves empty`() {
        val keep = ItemStack(Items.TORCH, 16)
        val current = mapOf("hotbar.0" to keep, "hotbar.1" to ItemStack(Items.DIRT, 5))
        val target = mapOf("hotbar.0" to keep.copy(), "armor.feet" to ItemStack(Items.IRON_BOOTS))
        val commands = InventoryPresets.loadCommands(current, target, registries)
        assertEquals(2, commands.size)
        assertEquals("item replace entity @s hotbar.1 with minecraft:air", commands[0])
        assertTrue(commands[1].startsWith("item replace entity @s armor.feet with minecraft:iron_boots"), commands[1])
        assertEquals(listOf("hotbar.1"), InventoryPresets.lostSlots(current, target))
    }

    @Test
    fun `loading into an empty inventory loses nothing`() {
        assertTrue(InventoryPresets.lostSlots(emptyMap(), mapOf("hotbar.0" to sword())).isEmpty())
    }

    @Test
    fun `clearing a slot sends air, which the item argument accepts`() {
        val command = ItemCommand.replace("weapon.offhand", ItemStack.EMPTY, registries)
        val reader = StringReader(command.removePrefix("item replace entity @s weapon.offhand with "))
        assertEquals(Items.AIR, ItemParser(registries).parse(reader).item().value())
    }
}
