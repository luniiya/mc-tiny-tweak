package dev.luniiya.mctinytweak.client.damagenumbers

import dev.luniiya.mctinytweak.client.tweaks.Tweaks
import dev.luniiya.mctinytweak.damagenumbers.DamageNumbers
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LightTexture
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.entity.LivingEntity
import net.minecraft.client.gui.Font

/** Spawns a floating number above living entities when their health drops, like hit numbers in gacha games. */
object DamageNumberPopups {
    /** Entities farther than this from the player do not get numbers. */
    private const val RANGE = 32.0

    /** The oldest numbers go first past this many. */
    private const val MAX_POPUPS = 64

    private class Popup(val x: Double, val y: Double, val z: Double, val text: FormattedCharSequence, val color: Int, val crit: Boolean) {
        var age = 0
    }

    private val health = DamageNumbers.HealthTracker()
    private val crits = DamageNumbers.Crits()
    private val popups = ArrayList<Popup>()
    private var level: ClientLevel? = null

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register(::tick)
        WorldRenderEvents.BEFORE_ENTITIES.register(::render)
    }

    /** Called when the server shows a critical hit on [entityId], just before its health update arrives. */
    @JvmStatic
    fun critical(entityId: Int) {
        val time = Minecraft.getInstance().level?.gameTime ?: return
        crits.mark(entityId, time)
    }

    private fun tick(minecraft: Minecraft) {
        val level = minecraft.level
        val player = minecraft.player
        if (level !== this.level || level == null || player == null || !Tweaks.damageNumbers()) {
            reset()
            this.level = level
            return
        }
        popups.forEach { it.age++ }
        popups.removeIf { it.age >= DamageNumbers.LIFETIME }

        val seen = HashSet<Int>()
        for (entity in level.entitiesForRendering()) {
            if (entity !is LivingEntity || entity === player || entity.distanceToSqr(player) > RANGE * RANGE) continue
            seen += entity.id
            val lost = health.update(entity.id, entity.health) ?: continue
            spawn(entity, lost, crits.take(entity.id, level.gameTime))
        }
        health.retain(seen)
        crits.prune(level.gameTime)
    }

    private fun spawn(entity: LivingEntity, damage: Float, crit: Boolean) {
        val random = entity.random
        val text = Component.literal(DamageNumbers.format(damage)).withStyle(ChatFormatting.BOLD).visualOrderText
        popups += Popup(
            entity.x + (random.nextDouble() - 0.5) * entity.bbWidth,
            entity.y + entity.bbHeight + 0.2,
            entity.z + (random.nextDouble() - 0.5) * entity.bbWidth,
            text,
            DamageNumbers.color(damage, crit),
            crit,
        )
        while (popups.size > MAX_POPUPS) popups.removeFirst()
    }

    private fun reset() {
        popups.clear()
        health.clear()
        crits.clear()
    }

    private fun render(context: WorldRenderContext) {
        if (popups.isEmpty()) return
        val minecraft = Minecraft.getInstance()
        val font = minecraft.font
        val camera = context.worldState().cameraRenderState
        val partial = minecraft.deltaTracker.getGameTimeDeltaPartialTick(false)
        val poseStack = context.matrices()
        for (popup in popups) {
            val age = popup.age + partial
            val alpha = DamageNumbers.alpha(age)
            if (alpha <= 0f) continue
            poseStack.pushPose()
            poseStack.translate(popup.x - camera.pos.x, popup.y + DamageNumbers.rise(age) - camera.pos.y, popup.z - camera.pos.z)
            poseStack.mulPose(camera.orientation)
            val scale = 0.025f * DamageNumbers.scale(age, popup.crit)
            poseStack.scale(scale, -scale, scale)
            val outline = DamageNumbers.withAlpha(ARGB.scaleRGB(popup.color, 0.25f), alpha)
            context.commandQueue().submitText(
                poseStack,
                -font.width(popup.text) / 2f,
                0f,
                popup.text,
                false,
                Font.DisplayMode.NORMAL,
                LightTexture.FULL_BRIGHT,
                DamageNumbers.withAlpha(popup.color, alpha),
                0,
                outline,
            )
            poseStack.popPose()
        }
    }
}
