package dev.luniiya.mctinytweak.mcp

import net.minecraft.core.Direction

/** Refuse mining that opens a lava face; water above can protect ordinary obsidian mining, not overhead lava. */
object MiningSafety {
    fun exposesLava(lavaFaces: Set<Direction>, waterAbove: Boolean): Boolean =
        Direction.UP in lavaFaces || (!waterAbove && lavaFaces.isNotEmpty())
}
