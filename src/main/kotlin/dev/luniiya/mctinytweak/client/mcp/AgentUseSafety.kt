package dev.luniiya.mctinytweak.client.mcp

import dev.luniiya.mctinytweak.mcp.ToolError
import dev.luniiya.mctinytweak.mcp.WaterPlacementSafety
import net.minecraft.client.Minecraft
import net.minecraft.tags.FluidTags
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.LiquidBlockContainer
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

object AgentUseSafety {
    fun prepare(minecraft: Minecraft) {
        val player = minecraft.player ?: return
        if (minecraft.screen != null) return
        if (!player.mainHandItem.`is`(Items.WATER_BUCKET) && !player.offhandItem.`is`(Items.WATER_BUCKET)) return
        val level = minecraft.level ?: return
        minecraft.gameRenderer.pick(1f)
        val hit = minecraft.hitResult as? BlockHitResult ?: return
        if (hit.type != HitResult.Type.BLOCK) return
        val target = level.getBlockState(hit.blockPos)
        val destination = if (target.block is LiquidBlockContainer) hit.blockPos else hit.blockPos.relative(hit.direction)
        val unsafe = WaterPlacementSafety.unsafeAbove(destination) { pos ->
            when {
                level.isOutsideBuildHeight(pos) -> WaterPlacementSafety.Cell.CLEAR
                level.chunkSource.getChunk(pos.x shr 4, pos.z shr 4, ChunkStatus.FULL, false) == null -> WaterPlacementSafety.Cell.UNKNOWN
                level.getFluidState(pos).`is`(FluidTags.LAVA) -> WaterPlacementSafety.Cell.LAVA
                else -> WaterPlacementSafety.Cell.CLEAR
            }
        }
        if (unsafe.isNotEmpty()) throw ToolError("Refused water placement: lava or unknown cells above/near the destination. Falling lava can replace the water source with stone and reroute toward you. Block the lava cascade or choose a flat pool; a block snapshot does not predict fluid changes.")
    }
}
