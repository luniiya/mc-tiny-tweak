package dev.luniiya.mctinytweak.client.inventorypresets

import dev.luniiya.mctinytweak.client.Notices
import dev.luniiya.mctinytweak.client.itemeditor.ItemEditorSession
import dev.luniiya.mctinytweak.inventorypresets.InventoryPresets
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.level.GameType

/**
 * Gives the starter kit once, on the first join of a world made from the Create World screen
 * in survival with commands on. Worlds made by the MCP `create_world` tool never get it.
 */
object StarterInventory {
    private var pending = false

    /** Called by the Create World screen after it has made the world. */
    @JvmStatic
    fun worldCreated(gameType: GameType, allowCommands: Boolean) {
        pending = InventoryPresets.getsStarterKit(gameType, allowCommands)
    }

    fun tick(minecraft: Minecraft) {
        if (!pending) return
        val player = minecraft.player ?: return
        // Give the server a second to finish sending the spawn state.
        if (player.tickCount < 20) return
        pending = false
        // Only the world that was just made, never a server joined after a failed creation.
        if (!minecraft.isLocalServer) return
        if (!ItemEditorSession.canApplyAsOperator(minecraft)) return
        if (InventoryPresetStore.snapshot(player).isNotEmpty()) return
        InventoryPresetStore.starter(player)?.let {
            InventoryPresetStore.give(player, it)
            Notices.show(Component.translatable("notice.mc_tiny_tweak.inventory_presets.kit_given"))
        }
    }
}
