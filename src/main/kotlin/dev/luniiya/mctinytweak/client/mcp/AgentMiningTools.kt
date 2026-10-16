package dev.luniiya.mctinytweak.client.mcp

import dev.luniiya.mctinytweak.mcp.MiningTools
import dev.luniiya.mctinytweak.mcp.ToolError
import net.minecraft.client.Minecraft
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

/** Direct MCP attacks use the same vanilla tool properties as manual mining; Baritone handles its own tools. */
object AgentMiningTools {
    fun prepare(minecraft: Minecraft) {
        val player = minecraft.player ?: throw ToolError("Not in a world")
        if (minecraft.screen != null) throw ToolError("Close the open screen before attacking")
        minecraft.gameRenderer.pick(1f)
        val hit = minecraft.hitResult as? BlockHitResult ?: return // Keep the chosen combat weapon for entities.
        if (hit.type != HitResult.Type.BLOCK) return
        val state = player.level().getBlockState(hit.blockPos)
        if (state.getDestroySpeed(player.level(), hit.blockPos) < 0) throw ToolError("The targeted block cannot be mined")
        val inventory = player.inventory
        val candidates = (0..35).map { slot ->
            val stack = inventory.getItem(slot)
            MiningTools.Candidate(slot, stack.getDestroySpeed(state), stack.isCorrectToolForDrops(state),
                if (stack.isDamageableItem) stack.maxDamage - stack.damageValue else null)
        }
        val best = MiningTools.best(candidates, inventory.selectedSlot, state.requiresCorrectToolForDrops())
            ?: throw ToolError("No available tool can harvest the targeted block")
        if (best.slot < 9) {
            inventory.selectedSlot = best.slot
        } else {
            if (player.containerMenu !== player.inventoryMenu) throw ToolError("Close the container before selecting a mining tool")
            minecraft.gameMode!!.handleInventoryMouseClick(player.inventoryMenu.containerId, best.slot, inventory.selectedSlot, ClickType.SWAP, player)
        }
    }
}
