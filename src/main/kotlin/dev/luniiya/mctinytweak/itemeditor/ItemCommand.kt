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
        require(stack.count in 1..MAX_COUNT) { "Count must be between 1 and $MAX_COUNT" }
        val item = ItemInput(stack.itemHolder, stack.componentsPatch).serialize(registries)
        return "item replace entity @s hotbar.$hotbarSlot with $item ${stack.count}"
    }
}
