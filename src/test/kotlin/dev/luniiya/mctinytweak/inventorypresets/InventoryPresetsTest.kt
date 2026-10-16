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
import net.minecraft.world.level.GameType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

    @Test
    fun `the bundled starter kit is the saved elytra and mace loadout`() {
        val kit = InventoryPresets.decode(InventoryPresets.bundled(), registries)[0]!!
        assertTrue(kit["inventory.0"]!!.`is`(Items.ELYTRA))
        val mace = kit["inventory.18"]!!
        assertTrue(mace.`is`(Items.MACE))
        val windBurst = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.WIND_BURST)
        assertEquals(255, mace.enchantments.getLevel(windBurst))
        assertTrue(kit["hotbar.1"]!!.`is`(Items.COPPER_PICKAXE))
        // Giving it to an empty inventory sends one command per kit slot.
        assertEquals(kit.size, InventoryPresets.loadCommands(emptyMap(), kit, registries).size)
    }

    @Test
    fun `only survival worlds with commands get the starter kit`() {
        assertTrue(InventoryPresets.getsStarterKit(GameType.SURVIVAL, allowCommands = true))
        assertFalse(InventoryPresets.getsStarterKit(GameType.SURVIVAL, allowCommands = false))
        assertFalse(InventoryPresets.getsStarterKit(GameType.CREATIVE, allowCommands = true))
        assertFalse(InventoryPresets.getsStarterKit(GameType.ADVENTURE, allowCommands = true))
    }

    @Test
    fun `names survive the file, stay through saving over, and go when the slot is cleared`() {
        val named = InventoryPresets().with(2, mapOf("hotbar.0" to sword()), "  pvp kit  ")
        assertEquals("pvp kit", named.name(2))
        val back = InventoryPresets.decode(named.encode(registries), registries)
        assertEquals("pvp kit", back.name(2))
        assertTrue(InventoryPresets.sameSlots(named[2]!!, back[2]!!))
        assertEquals("pvp kit", back.with(2, mapOf("hotbar.1" to ItemStack(Items.BOW))).name(2))
        assertNull(back.with(2, emptyMap()).name(2))
    }

    @Test
    fun `renaming trims, caps the length, and a blank name removes it`() {
        val presets = InventoryPresets().with(0, mapOf("hotbar.0" to sword()))
        assertNull(presets.name(0))
        assertEquals("x".repeat(InventoryPresets.MAX_NAME_LENGTH), presets.renamed(0, "x".repeat(50)).name(0))
        assertNull(presets.renamed(0, "mine").renamed(0, "   ").name(0))
        // An empty slot has nothing to name.
        assertNull(presets.renamed(5, "ghost").name(5))
    }

    @Test
    fun `files saved before names existed still load`() {
        // The bundled kit is in the old format: slots straight under the index.
        val old = InventoryPresets.bundled()
        assertTrue(old.getCompound("0").orElseThrow().contains("hotbar.0"))
        val presets = InventoryPresets.decode(old, registries)
        assertTrue(presets[0]!!["inventory.0"]!!.`is`(Items.ELYTRA))
        assertNull(presets.name(0))
    }
}
