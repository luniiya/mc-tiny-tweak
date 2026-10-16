package dev.luniiya.mctinytweak.mcp

import net.minecraft.core.BlockPos

/** A minimum ten-obsidian frame, with four ordinary-block corners for builder support. */
class NetherPortalPlan(val origin: BlockPos, val axis: String) {
    enum class Role { OBSIDIAN, CORNER, INTERIOR }
    data class Cell(val position: BlockPos, val role: Role)

    init {
        require(axis == "x" || axis == "z") { "axis must be x or z" }
        require(origin.x in -29_999_995..29_999_995 && origin.z in -29_999_995..29_999_995) { "Portal is too close to world bounds" }
        require(origin.y in -30_000_000..30_000_000) { "Invalid portal height" }
    }

    val widthX = if (axis == "x") 4 else 1
    val lengthZ = if (axis == "z") 4 else 1
    val cells: List<Cell> = buildList {
        for (y in 0..4) for (u in 0..3) {
            val edgeX = u == 0 || u == 3
            val edgeY = y == 0 || y == 4
            val role = when { edgeX && edgeY -> Role.CORNER; edgeX || edgeY -> Role.OBSIDIAN; else -> Role.INTERIOR }
            add(Cell(origin.offset(if (axis == "x") u else 0, y, if (axis == "z") u else 0), role))
        }
    }
    val ignitionBlock: BlockPos = origin.offset(if (axis == "x") 1 else 0, 0, if (axis == "z") 1 else 0)
    val approach: BlockPos = origin.offset(if (axis == "x") 1 else -1, 0, if (axis == "z") 1 else -1)
    fun role(x: Int, y: Int, z: Int): Role? = cells.firstOrNull { it.position == origin.offset(x, y, z) }?.role
}
