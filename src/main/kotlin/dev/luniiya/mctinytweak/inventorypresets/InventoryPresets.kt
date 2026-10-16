package dev.luniiya.mctinytweak.inventorypresets

import com.mojang.serialization.Codec
import dev.luniiya.mctinytweak.itemeditor.ItemCommand
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.world.item.ItemStack

/**
 * Whole-inventory loadouts kept in [SIZE] numbered slots, like a hotbar of kits.
 * A preset maps `/item` slot names (see [SLOT_NAMES]) to the stacks in them; empty slots are left out.
 */
class InventoryPresets(presets: List<Map<String, ItemStack>?> = emptyList()) {
    private val presets: List<Map<String, ItemStack>?> = List(SIZE) { i -> presets.getOrNull(i)?.takeIf { it.isNotEmpty() } }

    operator fun get(index: Int): Map<String, ItemStack>? = presets[index]

    /** Saving an empty inventory clears the slot. */
    fun with(index: Int, slots: Map<String, ItemStack>?): InventoryPresets {
        require(index in 0 until SIZE) { "Preset index must be between 0 and ${SIZE - 1}" }
        val clean = slots?.filterValues { !it.isEmpty }?.mapValues { it.value.copy() }
        return InventoryPresets(presets.toMutableList().also { it[index] = clean })
    }

    /** The item that stands for a preset in the bar: its first hotbar item, or else any item in it. */
    fun icon(index: Int): ItemStack {
        val slots = presets[index] ?: return ItemStack.EMPTY
        return SLOT_NAMES.firstNotNullOfOrNull { slots[it] } ?: ItemStack.EMPTY
    }

    /** Stores items with [registries] so enchantments and other registry data survive. */
    fun encode(registries: HolderLookup.Provider): CompoundTag {
        val ops = registries.createSerializationContext(NbtOps.INSTANCE)
        val root = CompoundTag()
        presets.forEachIndexed { i, slots ->
            if (slots != null) root.put(i.toString(), SLOTS_CODEC.encodeStart(ops, slots).getOrThrow())
        }
        return root
    }

    companion object {
        const val SIZE = 9
        const val FILE_NAME = "mc_tiny_tweak_inventory_presets.dat"

        /** Every slot a preset covers, in `/item` slot names: hotbar, main inventory, armor, offhand. */
        val SLOT_NAMES: List<String> = (0..8).map { "hotbar.$it" } + (0..26).map { "inventory.$it" } +
            listOf("armor.head", "armor.chest", "armor.legs", "armor.feet", "weapon.offhand")

        private val SLOTS_CODEC: Codec<Map<String, ItemStack>> = Codec.unboundedMap(Codec.STRING, ItemStack.CODEC)

        /** Reads presets, skipping any that no longer decode, for example items from a missing mod. */
        fun decode(tag: CompoundTag, registries: HolderLookup.Provider): InventoryPresets {
            val ops = registries.createSerializationContext(NbtOps.INSTANCE)
            val list = MutableList<Map<String, ItemStack>?>(SIZE) { null }
            for (key in tag.keySet()) {
                val index = key.toIntOrNull()?.takeIf { it in 0 until SIZE } ?: continue
                val value = tag.get(key) ?: continue
                list[index] = SLOTS_CODEC.parse(ops, value).result().orElse(null)?.filterKeys { it in SLOT_NAMES }
            }
            return InventoryPresets(list)
        }

        /** True when both hold the same stacks in the same slots. ItemStack has no content equals. */
        fun sameSlots(a: Map<String, ItemStack>, b: Map<String, ItemStack>): Boolean =
            SLOT_NAMES.all { ItemStack.matches(a[it] ?: ItemStack.EMPTY, b[it] ?: ItemStack.EMPTY) }

        /** Slots whose current stack would be replaced by something else, so loading would lose it. */
        fun lostSlots(current: Map<String, ItemStack>, target: Map<String, ItemStack>): List<String> =
            SLOT_NAMES.filter { slot ->
                val now = current[slot] ?: ItemStack.EMPTY
                !now.isEmpty && !ItemStack.matches(now, target[slot] ?: ItemStack.EMPTY)
            }

        /** `/item replace` commands that turn [current] into [target], skipping slots that already match. */
        fun loadCommands(current: Map<String, ItemStack>, target: Map<String, ItemStack>, registries: HolderLookup.Provider): List<String> =
            SLOT_NAMES.mapNotNull { slot ->
                val now = current[slot] ?: ItemStack.EMPTY
                val wanted = target[slot] ?: ItemStack.EMPTY
                if (ItemStack.matches(now, wanted)) null else ItemCommand.replace(slot, wanted, registries)
            }
    }
}
