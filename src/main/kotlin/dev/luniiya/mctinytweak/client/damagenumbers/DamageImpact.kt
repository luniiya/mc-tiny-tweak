package dev.luniiya.mctinytweak.client.damagenumbers

import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.damagenumbers.DamageNumbers
import dev.luniiya.mctinytweak.damagenumbers.DamageVfx
import dev.luniiya.mctinytweak.mixin.client.damagenumbers.GameRendererPostEffectInvoker
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.Identifier
import kotlin.math.sqrt

/**
 * The screen effects of the biggest hits, none of which move the camera: anime focus lines around the
 * screen edge, and for MAX hits an impact frame that inverts the world for an instant, then flashes white.
 */
object DamageImpact {
    private val INVERT = Identifier.withDefaultNamespace("invert")

    /** Game time the focus lines started, their tier and seed. */
    private var focusStart = Long.MIN_VALUE
    private var focusTier = 0
    private var focusSeed = 0

    /** Game time the impact frame started. */
    private var impactStart = Long.MIN_VALUE

    /** True while the world is inverted by us, so we only ever clear our own effect. */
    private var inverted = false

    fun register() {
        HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath(McTinyTweak.MOD_ID, "damage_impact"), ::render)
    }

    fun focus(time: Long, tier: Int, seed: Int) {
        focusStart = time
        focusTier = tier
        focusSeed = seed
    }

    fun impact(time: Long) {
        impactStart = time
        val renderer = Minecraft.getInstance().gameRenderer
        // Spectating a creeper, spider or enderman already has an effect; leave it alone then.
        if (!inverted && renderer.currentPostEffect() == null) {
            (renderer as GameRendererPostEffectInvoker).`mc_tiny_tweak$setPostEffect`(INVERT)
            inverted = true
        }
    }

    /** Ends the inverted frame on time, on ticks so it ends even with the HUD hidden. */
    fun tick(time: Long) {
        if (inverted && !DamageVfx.impactInverted((time - impactStart).toFloat())) stop()
    }

    fun reset() {
        focusStart = Long.MIN_VALUE
        impactStart = Long.MIN_VALUE
        stop()
    }

    private fun stop() {
        if (!inverted) return
        inverted = false
        val renderer = Minecraft.getInstance().gameRenderer
        if (renderer.currentPostEffect() == INVERT) renderer.clearPostEffect()
    }

    private fun render(graphics: GuiGraphics, delta: DeltaTracker) {
        val level = Minecraft.getInstance().level ?: return
        val partial = delta.getGameTimeDeltaPartialTick(false)
        val width = graphics.guiWidth()
        val height = graphics.guiHeight()

        val focusAge = level.gameTime - focusStart + partial
        val focusAlpha = if (focusStart == Long.MIN_VALUE) 0f else DamageVfx.focusAlpha(focusAge)
        if (focusAlpha > 0f) {
            val count = DamageVfx.focusLines(focusTier)
            val reach = sqrt((width * width + height * height).toFloat()) / 2f
            val tick = focusAge.toInt()
            val color = DamageNumbers.withAlpha(DamageNumbers.WHITE, 0.85f * focusAlpha)
            graphics.pose().pushMatrix()
            graphics.pose().translate(width / 2f, height / 2f)
            for (i in 0 until count) {
                val (angle, depth) = DamageVfx.focusLine(i, count, tick, focusSeed)
                val thickness = 1 + (i * 7 + tick) % 3
                graphics.pose().pushMatrix()
                graphics.pose().rotate(angle)
                graphics.fill((reach * (1f - depth)).toInt(), -thickness / 2, (reach + 4).toInt(), thickness - thickness / 2, color)
                graphics.pose().popMatrix()
            }
            graphics.pose().popMatrix()
        }

        if (impactStart != Long.MIN_VALUE) {
            val flash = DamageVfx.impactFlash(level.gameTime - impactStart + partial)
            if (flash > 0f) graphics.fill(0, 0, width, height, DamageNumbers.withAlpha(DamageNumbers.WHITE, flash))
        }
    }
}
