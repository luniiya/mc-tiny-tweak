package dev.luniiya.mctinytweak.client.itemeditor

import dev.luniiya.mctinytweak.itemeditor.ItemDraft
import net.minecraft.client.Minecraft
import net.minecraft.core.HolderLookup
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/** State shared by the item editor screens while one held item is being edited. */
class ItemEditorSession(
    var base: ItemStack,
    val slot: Int,
    val registries: HolderLookup.Provider,
) {
    var draft: ItemDraft = ItemDraft.from(base)
        set(value) {
            field = value
            result = value.build(base, registries)
        }

    var result: ItemDraft.Result = draft.build(base, registries)
        private set

    /** The stack to show: the built draft when valid, otherwise the last good base. */
    val preview: ItemStack
        get() = (result as? ItemDraft.Result.Success)?.stack ?: base

    /** Replaces the whole item, for example after raw SNBT edits. */
    fun reset(stack: ItemStack) {
        base = stack
        draft = ItemDraft.from(stack)
    }

    /** Writes the built item into the held slot. Returns a message describing the outcome. */
    fun apply(minecraft: Minecraft): Component {
        val stack = (result as? ItemDraft.Result.Success)?.stack
            ?: return Component.translatable("screen.mc_tiny_tweak.item_editor.invalid")
        val player = minecraft.player ?: return Component.translatable("screen.mc_tiny_tweak.item_editor.no_player")
        if (!player.hasInfiniteMaterials()) return Component.translatable("screen.mc_tiny_tweak.item_editor.creative_only")
        player.inventory.setItem(slot, stack.copy())
        minecraft.gameMode?.handleCreativeModeItemAdd(stack.copy(), HOTBAR_MENU_OFFSET + slot)
        reset(stack)
        return Component.translatable("screen.mc_tiny_tweak.item_editor.applied")
    }

    companion object {
        /** Hotbar slot 0 is menu slot 36 in the player's inventory menu. */
        const val HOTBAR_MENU_OFFSET = 36

        fun forHeldItem(minecraft: Minecraft): ItemEditorSession {
            val player = checkNotNull(minecraft.player) { "The item editor needs a player" }
            val held = player.inventory.selectedItem
            return ItemEditorSession(
                base = if (held.isEmpty) ItemStack(Items.STONE) else held.copy(),
                slot = player.inventory.selectedSlot,
                registries = player.registryAccess(),
            )
        }
    }
}
