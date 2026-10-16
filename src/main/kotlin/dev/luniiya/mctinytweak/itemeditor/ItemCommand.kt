package dev.luniiya.mctinytweak.itemeditor

import net.minecraft.commands.arguments.item.ItemInput
import net.minecraft.core.HolderLookup
import net.minecraft.world.item.ItemStack

/** Builds the operator command that puts an edited item into a hotbar slot, for players not in creative mode. */
object ItemCommand {
    /** The largest count `/item replace` accepts. */
    const val MAX_COUNT = 99

    /** Returns the command without its leading slash, as `ClientPacketListener.sendCommand` expects. */
    fun replaceHotbar(stack: ItemStack, hotbarSlot: Int, registries: HolderLookup.Provider): String {
        require(hotbarSlot in 0..8) { "Hotbar slot must be between 0 and 8" }
        return replace("hotbar.$hotbarSlot", stack, registries)
    }

    /** Puts [stack] into a `/item` slot such as `inventory.3` or `armor.head`. An empty stack clears the slot. */
    fun replace(slot: String, stack: ItemStack, registries: HolderLookup.Provider): String {
        if (stack.isEmpty) return "item replace entity @s $slot with minecraft:air"
        require(stack.count in 1..MAX_COUNT) { "Count must be between 1 and $MAX_COUNT" }
        val item = ItemInput(stack.itemHolder, stack.componentsPatch).serialize(registries)
        return "item replace entity @s $slot with $item ${stack.count}"
    }
}
