package dev.luniiya.mctinytweak.client.inventorypresets

import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.inventorypresets.InventoryPresets
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.player.LocalPlayer
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import java.nio.file.Files
import java.nio.file.Path

/** Keeps the inventory presets on disk and loads them into the player with `/item replace`. */
object InventoryPresetStore {
    private val file: Path get() = FabricLoader.getInstance().configDir.resolve(InventoryPresets.FILE_NAME)

    /** Raw saved data; items are decoded with the current world's registries each time. */
    private var tag: CompoundTag? = null

    /** How many `/item` success messages to hide from chat after a load, so it does not flood. */
    private var hiddenFeedback = 0

    fun register() {
        ClientReceiveMessageEvents.ALLOW_GAME.register { message, overlay ->
            val key = (message.contents as? TranslatableContents)?.key
            if (!overlay && hiddenFeedback > 0 && key == "commands.item.entity.set.success.single") {
                hiddenFeedback--
                false
            } else {
                true
            }
        }
    }

    /** Decoded presets for the registries they were decoded with; the bar asks every frame. */
    private var cache: Pair<Any, InventoryPresets>? = null

    fun presets(player: LocalPlayer): InventoryPresets {
        val registries = player.registryAccess()
        cache?.takeIf { it.first === registries }?.let { return it.second }
        return InventoryPresets.decode(raw(), registries).also { cache = registries to it }
    }

    fun save(player: LocalPlayer, index: Int) {
        val updated = presets(player).with(index, snapshot(player))
        tag = updated.encode(player.registryAccess())
        cache = player.registryAccess() to updated
        try {
            Files.createDirectories(file.parent)
            NbtIo.writeCompressed(tag!!, file)
        } catch (e: Exception) {
            McTinyTweak.logger.warn("Could not save {}", file, e)
        }
    }

    /** What the player has now, in the preset's slot names. */
    fun snapshot(player: LocalPlayer): Map<String, ItemStack> {
        val inventory = player.inventory
        val slots = LinkedHashMap<String, ItemStack>()
        for (i in 0..8) slots["hotbar.$i"] = inventory.getItem(i)
        for (i in 0..26) slots["inventory.$i"] = inventory.getItem(9 + i)
        slots["armor.head"] = player.getItemBySlot(EquipmentSlot.HEAD)
        slots["armor.chest"] = player.getItemBySlot(EquipmentSlot.CHEST)
        slots["armor.legs"] = player.getItemBySlot(EquipmentSlot.LEGS)
        slots["armor.feet"] = player.getItemBySlot(EquipmentSlot.FEET)
        slots["weapon.offhand"] = player.getItemBySlot(EquipmentSlot.OFFHAND)
        return slots.filterValues { !it.isEmpty }.mapValues { it.value.copy() }
    }

    fun load(player: LocalPlayer, index: Int) {
        val target = presets(player)[index] ?: return
        val commands = InventoryPresets.loadCommands(snapshot(player), target, player.registryAccess())
        hiddenFeedback += commands.size
        commands.forEach(player.connection::sendCommand)
    }

    private fun raw(): CompoundTag {
        tag?.let { return it }
        val read = try {
            if (Files.exists(file)) NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()) else CompoundTag()
        } catch (e: Exception) {
            McTinyTweak.logger.warn("Could not read {}, starting with no inventory presets", file, e)
            CompoundTag()
        }
        tag = read
        return read
    }
}
