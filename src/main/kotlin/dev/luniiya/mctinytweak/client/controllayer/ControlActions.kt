package dev.luniiya.mctinytweak.client.controllayer

import dev.luniiya.mctinytweak.client.HiddenCommandFeedback
import dev.luniiya.mctinytweak.client.Notices
import dev.luniiya.mctinytweak.controllayer.ControlCommands
import dev.luniiya.mctinytweak.controllayer.ControlLayer
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.server.permissions.Permissions
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.phys.HitResult

/** Runs the Ctrl layer's shortcuts. Everything goes through vanilla OP commands; nothing runs on the server. */
object ControlActions {
    /** How far Ctrl+K reaches, in blocks. Blocks in the way stop it. */
    const val KILL_RANGE = 128.0

    private val layer = ControlLayer()

    /** Called from the keyboard mixin. True when the key belongs to the Ctrl layer and must not reach the game. */
    @JvmStatic
    fun onKey(key: Int, action: Int, ctrlDown: Boolean): Boolean {
        val minecraft = Minecraft.getInstance()
        val result = layer.onKey(key, action, ctrlDown, minecraft.screen != null)
        result.run?.let { run(minecraft, it) }
        return result.consume
    }

    private fun run(minecraft: Minecraft, action: ControlLayer.Action) {
        val player = minecraft.player ?: return
        if (!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            Notices.show(Component.translatable("notice.mc_tiny_tweak.control.needs_op"))
            return
        }
        when (action) {
            ControlLayer.Action.HEAL -> {
                HiddenCommandFeedback.hide(listOf("commands.effect.give.success.single"), 1)
                player.connection.sendCommand(ControlCommands.HEAL)
            }
            ControlLayer.Action.KILL -> {
                val target = crosshairEntity(minecraft)
                if (target == null) Notices.show(Component.translatable("notice.mc_tiny_tweak.control.no_target"))
                else player.connection.sendCommand(ControlCommands.kill(target.stringUUID))
            }
            ControlLayer.Action.SPECTATOR ->
                player.connection.sendCommand(ControlCommands.toggleSpectator(minecraft.gameMode?.playerMode))
        }
    }

    /** The entity under the crosshair up to [KILL_RANGE], seen from the camera (so freecam aims too). */
    private fun crosshairEntity(minecraft: Minecraft): Entity? {
        val camera = minecraft.cameraEntity ?: return null
        val eye = camera.getEyePosition(1f)
        val look = camera.getViewVector(1f)
        val block = camera.pick(KILL_RANGE, 1f, false)
        val reachSqr = if (block.type == HitResult.Type.MISS) KILL_RANGE * KILL_RANGE else block.location.distanceToSqr(eye)
        val box = camera.boundingBox.expandTowards(look.scale(KILL_RANGE)).inflate(1.0)
        return ProjectileUtil.getEntityHitResult(camera, eye, eye.add(look.scale(KILL_RANGE)), box, { candidate ->
            candidate !== minecraft.player && candidate !== camera && !candidate.isSpectator && candidate.isPickable
        }, reachSqr)?.entity
    }
}
