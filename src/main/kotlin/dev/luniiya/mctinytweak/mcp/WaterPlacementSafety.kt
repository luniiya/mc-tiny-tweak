package dev.luniiya.mctinytweak.mcp

import net.minecraft.core.BlockPos

/** Lava flowing down onto water replaces it with stone and can divert the flow toward the player. */
object WaterPlacementSafety {
    enum class Cell { CLEAR, LAVA, UNKNOWN }

    fun unsafeAbove(destination: BlockPos, read: (BlockPos) -> Cell): List<BlockPos> = buildList {
        for (y in 1..3) for (z in -1..1) for (x in -1..1) {
            val pos = destination.offset(x, y, z)
            if (read(pos) != Cell.CLEAR) add(pos)
        }
    }
}
