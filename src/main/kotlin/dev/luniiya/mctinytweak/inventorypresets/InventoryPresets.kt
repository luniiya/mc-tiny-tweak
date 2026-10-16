package dev.luniiya.mctinytweak.inventorypresets

import com.mojang.serialization.Codec
import dev.luniiya.mctinytweak.itemeditor.ItemCommand
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtOps
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType

/**
 * Whole-inventory loadouts kept in [SIZE] numbered slots, like a hotbar of kits.
 * A preset maps `/item` slot names (see [SLOT_NAMES]) to the stacks in them; empty slots are left out.
 */
class InventoryPresets(presets: List<Map<String, ItemStack>?> = emptyList(), names: List<String?> = emptyList()) {
    private val presets: List<Map<String, ItemStack>?> = List(SIZE) { i -> presets.getOrNull(i)?.takeIf { it.isNotEmpty() } }

    /** Optional names; an empty slot has none. */
    private val names: List<String?> = List(SIZE) { i -> if (this.presets[i] == null) null else cleanName(names.getOrNull(i)) }

    operator fun get(index: Int): Map<String, ItemStack>? = presets[index]

    fun name(index: Int): String? = names[index]

    /**
     * Saving an empty inventory clears the slot. The name is kept when [name] is null, so saving
     * over a preset does not lose it.
     */
    fun with(index: Int, slots: Map<String, ItemStack>?, name: String? = null): InventoryPresets {
        require(index in 0 until SIZE) { "Preset index must be between 0 and ${SIZE - 1}" }
        val clean = slots?.filterValues { !it.isEmpty }?.mapValues { it.value.copy() }
        return InventoryPresets(
            presets.toMutableList().also { it[index] = clean },
            names.toMutableList().also { it[index] = name ?: names[index] },
        )
    }

    /** A blank name removes it. Empty slots cannot be named. */
    fun renamed(index: Int, name: String): InventoryPresets {
        require(index in 0 until SIZE) { "Preset index must be between 0 and ${SIZE - 1}" }
        return InventoryPresets(presets, names.toMutableList().also { it[index] = cleanName(name) })
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
            if (slots == null) return@forEachIndexed
            val entry = CompoundTag()
            entry.put(SLOTS_KEY, SLOTS_CODEC.encodeStart(ops, slots).getOrThrow())
            names[i]?.let { entry.putString(NAME_KEY, it) }
            root.put(i.toString(), entry)
        }
        return root
    }

    companion object {
        const val SIZE = 9
        const val MAX_NAME_LENGTH = 32
        private const val SLOTS_KEY = "slots"
        private const val NAME_KEY = "name"

        fun cleanName(name: String?): String? = name?.trim()?.take(MAX_NAME_LENGTH)?.takeIf { it.isNotEmpty() }
        const val FILE_NAME = "mc_tiny_tweak_inventory_presets.dat"

        /** A presets file whose first preset is the starter kit, the locked slot next to the bar. */
        const val STARTER_RESOURCE = "/mc_tiny_tweak/starter-inventory.dat"

        fun bundled(): CompoundTag =
            InventoryPresets::class.java.getResourceAsStream(STARTER_RESOURCE)?.use { NbtIo.readCompressed(it, NbtAccounter.unlimitedHeap()) }
                ?: CompoundTag()

        /** The starter kit is for new survival worlds where you can use commands, never for creative or plain survival. */
        fun getsStarterKit(gameType: GameType, allowCommands: Boolean): Boolean = gameType == GameType.SURVIVAL && allowCommands

        /** Every slot a preset covers, in `/item` slot names: hotbar, main inventory, armor, offhand. */
        val SLOT_NAMES: List<String> = (0..8).map { "hotbar.$it" } + (0..26).map { "inventory.$it" } +
            listOf("armor.head", "armor.chest", "armor.legs", "armor.feet", "weapon.offhand")

        private val SLOTS_CODEC: Codec<Map<String, ItemStack>> = Codec.unboundedMap(Codec.STRING, ItemStack.CODEC)

        /** Reads presets, skipping any that no longer decode, for example items from a missing mod. */
        fun decode(tag: CompoundTag, registries: HolderLookup.Provider): InventoryPresets {
            val ops = registries.createSerializationContext(NbtOps.INSTANCE)
            val list = MutableList<Map<String, ItemStack>?>(SIZE) { null }
            val names = MutableList<String?>(SIZE) { null }
            for (key in tag.keySet()) {
                val index = key.toIntOrNull()?.takeIf { it in 0 until SIZE } ?: continue
                val value = tag.get(key) as? CompoundTag ?: continue
                // Older files stored the slots directly, with no name.
                val slots = if (value.contains(SLOTS_KEY)) value.get(SLOTS_KEY) else value
                list[index] = slots?.let { SLOTS_CODEC.parse(ops, it).result().orElse(null) }?.filterKeys { it in SLOT_NAMES }
                names[index] = value.getString(NAME_KEY).orElse(null)
            }
            return InventoryPresets(list, names)
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
