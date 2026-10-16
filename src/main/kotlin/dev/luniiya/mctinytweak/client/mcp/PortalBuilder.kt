package dev.luniiya.mctinytweak.client.mcp

import baritone.api.BaritoneAPI
import baritone.api.schematic.ISchematic
import com.google.gson.JsonObject
import dev.luniiya.mctinytweak.mcp.McpSettings.Feature
import dev.luniiya.mctinytweak.mcp.NetherPortalPlan
import dev.luniiya.mctinytweak.mcp.NetherPortalPlan.Role
import dev.luniiya.mctinytweak.mcp.ToolError
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.tags.FluidTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.sqrt

/** One survival build at a time. It never grants blocks, teleports or changes the world through commands. */
object PortalBuilder {
    private data class Job(val plan: NetherPortalPlan, val level: ClientLevel, val ignite: Boolean,
        var phase: String = "building", var message: String = "Building frame", var ticks: Int = 0, var attempts: Int = 0, var lastUse: Int = -20, var protectedObsidian: Boolean = false)
    private var job: Job? = null
    private val running = setOf("building", "approaching", "lighting")

    fun start(minecraft: Minecraft, plan: NetherPortalPlan, ignite: Boolean): JsonObject {
        if (!BaritoneBridge.installed) throw ToolError("Baritone is not installed")
        if (job?.phase in running) throw ToolError("A portal build is already running; cancel it first")
        val player = minecraft.player ?: throw ToolError("Not in a world")
        val level = minecraft.level ?: throw ToolError("Not in a world")
        if (player.isDeadOrDying) throw ToolError("Respawn before building")
        if (minecraft.screen != null) throw ToolError("Close the open screen before building")
        if (ignite && !Mcp.settings.allows(Feature.INPUT)) throw ToolError("Enable input permission for automatic ignition, or set ignite=false")
        if (player.blockPosition().distSqr(plan.origin) > 16.0 * 16.0) throw ToolError("Choose a portal site within 16 blocks so it can be inspected first")
        if (level.isOutsideBuildHeight(plan.origin) || level.isOutsideBuildHeight(plan.origin.above(4))) throw ToolError("Portal exceeds dimension build height")
        validateSite(level, plan)
        val missingObsidian = plan.cells.count { it.role == Role.OBSIDIAN && !level.getBlockState(it.position).`is`(Blocks.OBSIDIAN) }
        val missingCorners = plan.cells.count { it.role == Role.CORNER && level.getBlockState(it.position).canBeReplaced() }
        fun count(item: net.minecraft.world.item.Item) = (0..35).sumOf { slot -> player.inventory.getItem(slot).let { if (it.`is`(item)) it.count else 0 } }
        if (count(Items.OBSIDIAN) < missingObsidian) throw ToolError("Need $missingObsidian carried obsidian for this frame; found ${count(Items.OBSIDIAN)}")
        if (count(Items.COBBLESTONE) < missingCorners) throw ToolError("Need $missingCorners cobblestone for frame corners; extra scaffold blocks may also be needed")
        if (ignite && count(Items.FLINT_AND_STEEL) == 0 && !active(level, plan)) throw ToolError("Carry flint_and_steel to light the completed frame")
        if (ignite && (0..35).none { player.inventory.getItem(it).isCorrectToolForDrops(Blocks.COBBLESTONE.defaultBlockState()) })
            throw ToolError("Carry a pickaxe so Baritone can remove scaffolding and descend after building")
        val next = Job(plan, level, ignite)
        if (active(level, plan)) { next.phase = "complete"; next.message = "Portal is already active"; job = next; return status() }
        val schematic = object : ISchematic {
            override fun widthX() = plan.widthX
            override fun heightY() = 5
            override fun lengthZ() = plan.lengthZ
            override fun desiredState(x: Int, y: Int, z: Int, current: BlockState, available: List<BlockState>): BlockState = when (plan.role(x, y, z)) {
                Role.OBSIDIAN -> Blocks.OBSIDIAN.defaultBlockState()
                Role.CORNER -> if (current.canBeReplaced()) Blocks.COBBLESTONE.defaultBlockState() else current
                Role.INTERIOR -> if (current.`is`(Blocks.NETHER_PORTAL)) current else Blocks.AIR.defaultBlockState()
                null -> current
            }
        }
        AgentInput.releaseAll()
        BaritoneBridge.stop()
        val protected = BaritoneAPI.getSettings().blocksToDisallowBreaking
        if (Blocks.OBSIDIAN !in protected.value) {
            protected.value = protected.value + Blocks.OBSIDIAN
            next.protectedObsidian = true
        }
        job = next
        try { BaritoneAPI.getProvider().primaryBaritone.builderProcess.build("MCP Nether portal", schematic, plan.origin) }
        catch (error: Exception) { fail(error.message ?: "Could not start portal builder"); throw error }
        return status()
    }

    private fun validateSite(level: ClientLevel, plan: NetherPortalPlan) {
        for (x in -1..plan.widthX) for (y in -1..5) for (z in -1..plan.lengthZ) {
            val pos = plan.origin.offset(x, y, z)
            if (level.isOutsideBuildHeight(pos) || level.chunkSource.getChunk(pos.x shr 4, pos.z shr 4, ChunkStatus.FULL, false) == null)
                throw ToolError("Portal site contains unknown/out-of-height cells at $pos")
            if (level.getFluidState(pos).`is`(FluidTags.LAVA)) throw ToolError("Lava is too close to the portal site at $pos; choose a dry site")
        }
        for (cell in plan.cells) {
            val state = level.getBlockState(cell.position)
            if (!state.fluidState.isEmpty) throw ToolError("Portal site contains fluid at ${cell.position}")
            val allowed = state.canBeReplaced() || when (cell.role) {
                Role.OBSIDIAN -> state.`is`(Blocks.OBSIDIAN)
                Role.CORNER -> state.isFaceSturdy(level, cell.position, Direction.UP)
                Role.INTERIOR -> state.`is`(Blocks.NETHER_PORTAL)
            }
            if (!allowed) throw ToolError("Portal would replace an existing block at ${cell.position}; choose a clear site")
            if (cell.position.y == plan.origin.y && !level.getBlockState(cell.position.below()).isFaceSturdy(level, cell.position.below(), Direction.UP))
                throw ToolError("Portal needs solid ground under the entire bottom row")
        }
    }

    fun cancel(message: String = "Cancelled", stop: Boolean = true) {
        val current = job ?: return
        if (current.phase !in running) return
        current.phase = "cancelled"; current.message = message
        if (stop && BaritoneBridge.installed) BaritoneBridge.stop()
        restoreProtection(current)
    }

    fun tick(minecraft: Minecraft) {
        val current = job ?: return
        if (current.phase !in running) return
        val player = minecraft.player
        if (player == null || minecraft.level !== current.level || !AgentControl.agentInControl() || !Mcp.settings.enabled ||
            !Mcp.settings.allows(Feature.BARITONE) || !Mcp.settings.allows(Feature.TAKE_OVER)) {
            cancel("Control, connection or permission changed"); return
        }
        if (current.ignite && !Mcp.settings.allows(Feature.INPUT)) { cancel("Input permission disabled"); return }
        if (player.isDeadOrDying || player.isInLava || player.isOnFire) { fail("Player is dead or in a fire/lava hazard"); return }
        if (++current.ticks > 6000) { fail("Portal build timed out; inspect the site and Baritone feedback"); return }
        if (current.ticks % 5 != 0) return
        try {
            if (active(current.level, current.plan)) { finish("complete", "Portal active"); return }
            when (current.phase) {
                "building" -> if (frameReady(current.level, current.plan)) {
                    BaritoneBridge.stop()
                    if (!current.ignite) { finish("frame_ready", "Frame complete; ignition disabled"); return }
                    current.phase = "approaching"; current.message = "Approaching to light frame"
                    val p = current.plan.approach
                    BaritoneBridge.execute("goto ${p.x} ${p.y} ${p.z}")
                } else if (current.ticks > 20 && !BaritoneAPI.getProvider().primaryBaritone.builderProcess.isActive) {
                    fail("Baritone stopped before completing the frame; inspect its feedback")
                }
                "approaching", "lighting" -> {
                    val bottom = current.plan.ignitionBlock
                    if (!frameReady(current.level, current.plan)) { fail("Frame changed before ignition"); return }
                    if (!player.isWithinBlockInteractionRange(bottom, 0.0)) {
                        if (current.phase == "lighting" || !BaritoneAPI.getProvider().primaryBaritone.customGoalProcess.isActive)
                            fail("Cannot reach the ignition block safely")
                        return
                    }
                    if (minecraft.screen != null || current.ticks - current.lastUse < 20) return
                    if (current.attempts >= 3) { fail("Server did not activate the portal after ignition; inspect frame and permissions"); return }
                    validateSite(current.level, current.plan)
                    BaritoneBridge.stop()
                    val slot = (0..35).firstOrNull { player.inventory.getItem(it).`is`(Items.FLINT_AND_STEEL) }
                        ?: throw ToolError("Flint-and-steel is no longer in inventory")
                    if (slot < 9) player.inventory.selectedSlot = slot else minecraft.gameMode!!.handleInventoryMouseClick(0, slot, player.inventory.selectedSlot, ClickType.SWAP, player)
                    val hit = Vec3(bottom.x + .5, bottom.y + 1.0, bottom.z + .5)
                    val delta = hit.subtract(player.eyePosition)
                    player.yRot = Math.toDegrees(atan2(-delta.x, delta.z)).toFloat()
                    player.xRot = Math.toDegrees(-atan2(delta.y, sqrt(delta.x * delta.x + delta.z * delta.z))).toFloat()
                    minecraft.gameMode!!.useItemOn(player, InteractionHand.MAIN_HAND, BlockHitResult(hit, Direction.UP, bottom, false))
                    player.swing(InteractionHand.MAIN_HAND)
                    current.phase = "lighting"; current.message = "Waiting for portal activation"
                    current.lastUse = current.ticks; current.attempts++
                }
            }
        } catch (error: Exception) { fail(error.message ?: "Portal build failed") }
    }

    private fun frameReady(level: ClientLevel, plan: NetherPortalPlan) = plan.cells.all {
        val state = level.getBlockState(it.position)
        when (it.role) {
            Role.OBSIDIAN -> state.`is`(Blocks.OBSIDIAN)
            Role.CORNER -> true // Corners are construction supports; a valid portal does not need them.
            Role.INTERIOR -> state.isAir || state.`is`(Blocks.NETHER_PORTAL) || state.`is`(Blocks.FIRE)
        }
    }
    private fun active(level: ClientLevel, plan: NetherPortalPlan) = plan.cells.filter { it.role == Role.INTERIOR }.all { level.getBlockState(it.position).`is`(Blocks.NETHER_PORTAL) }
    private fun fail(message: String) = finish("failed", message)
    private fun finish(phase: String, message: String) {
        job?.let { it.phase = phase; it.message = message }
        if (BaritoneBridge.installed) BaritoneBridge.stop()
        job?.let { restoreProtection(it) }
        Mcp.chat.add("[Portal builder] $message")
    }
    private fun restoreProtection(current: Job) {
        if (!current.protectedObsidian || !BaritoneBridge.installed) return
        val setting = BaritoneAPI.getSettings().blocksToDisallowBreaking
        setting.value = setting.value.filter { it !== Blocks.OBSIDIAN }
        current.protectedObsidian = false
    }
    fun status(): JsonObject = JsonObject().apply {
        val current = job
        addProperty("phase", current?.phase ?: "idle")
        addProperty("message", current?.message ?: "No portal build started")
        current?.let {
            addProperty("x", it.plan.origin.x); addProperty("y", it.plan.origin.y); addProperty("z", it.plan.origin.z)
            addProperty("axis", it.plan.axis); addProperty("ticks", it.ticks); addProperty("ignition_attempts", it.attempts)
            addProperty("obsidian_placed", it.plan.cells.count { cell -> cell.role == Role.OBSIDIAN && it.level.getBlockState(cell.position).`is`(Blocks.OBSIDIAN) })
        }
    }
}
