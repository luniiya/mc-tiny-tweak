package dev.luniiya.mctinytweak.mcp

import net.minecraft.core.BlockPos

/** Unknown cells must never be reported as safe air. Reads are bounded to 729 cells. */
object BlockInspection {
    data class Block(val id: String, val air: Boolean, val fluid: String?, val fluidSource: Boolean, val state: String)
    data class Cell(val position: BlockPos, val block: Block?)

    fun inspect(center: BlockPos, radius: Int, includeAir: Boolean, read: (BlockPos) -> Block?): List<Cell> {
        require(radius in 0..4) { "radius must be between 0 and 4" }
        return buildList {
            for (y in -radius..radius) for (z in -radius..radius) for (x in -radius..radius) {
                val pos = center.offset(x, y, z)
                val block = read(pos)
                if (block == null || includeAir || !block.air || block.fluid != null) add(Cell(pos, block))
            }
        }
    }
}
