package dev.luniiya.mctinytweak.gametest

import dev.luniiya.mctinytweak.mcp.NetherPortalPlan
import dev.luniiya.mctinytweak.mcp.WaterPlacementSafety
import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.block.Blocks

class PortalGameTests {
    private fun frame(helper: GameTestHelper, axis: String): NetherPortalPlan {
        val plan = NetherPortalPlan(BlockPos(2, 1, 2), axis)
        for (cell in plan.cells) helper.setBlock(cell.position, if (cell.role == NetherPortalPlan.Role.OBSIDIAN) Blocks.OBSIDIAN else Blocks.AIR)
        return plan
    }

    private fun ignition(helper: GameTestHelper, axis: String) {
        val plan = frame(helper, axis)
        helper.setBlock(plan.ignitionBlock.above(), Blocks.FIRE)
        helper.runAfterDelay(1) {
            for (cell in plan.cells.filter { it.role == NetherPortalPlan.Role.INTERIOR }) helper.assertBlockPresent(Blocks.NETHER_PORTAL, cell.position)
            helper.succeed()
        }
    }

    @GameTest
    fun minimumXFrameIgnitesWithoutDecorativeCorners(helper: GameTestHelper) = ignition(helper, "x")

    @GameTest
    fun minimumZFrameIgnitesWithoutDecorativeCorners(helper: GameTestHelper) = ignition(helper, "z")

    @GameTest
    fun aFrameBrokenDuringDescentCannotIgnite(helper: GameTestHelper) {
        val plan = frame(helper, "x")
        helper.setBlock(plan.origin.offset(1, 4, 0), Blocks.AIR)
        helper.setBlock(plan.ignitionBlock.above(), Blocks.FIRE)
        helper.runAfterDelay(1) {
            helper.assertTrue(plan.cells.filter { it.role == NetherPortalPlan.Role.INTERIOR }.none {
                helper.getBlockState(it.position).`is`(Blocks.NETHER_PORTAL)
            }, "Broken obsidian frame must not activate")
            helper.succeed()
        }
    }

    @GameTest(maxTicks = 100)
    fun fallingLavaDestroysWaterProtectionAndIsRefused(helper: GameTestHelper) {
        val water = BlockPos(3, 2, 3)
        helper.setBlock(water.below(), Blocks.STONE)
        helper.setBlock(water, Blocks.WATER)
        helper.setBlock(water.above(), Blocks.LAVA)
        val unsafe = WaterPlacementSafety.unsafeAbove(water) { pos ->
            if (helper.getBlockState(pos).fluidState.`is`(FluidTags.LAVA)) WaterPlacementSafety.Cell.LAVA else WaterPlacementSafety.Cell.CLEAR
        }
        helper.assertTrue(water.above() in unsafe, "Use guard must reject real falling lava above water")
        helper.runAfterDelay(60) {
            helper.assertBlockPresent(Blocks.STONE, water)
            helper.succeed()
        }
    }
}
