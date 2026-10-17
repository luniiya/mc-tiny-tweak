package dev.luniiya.mctinytweak.client.damagenumbers

import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.client.tweaks.Tweaks
import dev.luniiya.mctinytweak.damagenumbers.DamageNumberStyle
import dev.luniiya.mctinytweak.damagenumbers.DamageNumbers
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LightTexture
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.util.ARGB
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.entity.LivingEntity
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Spawns a floating number above living entities when their health drops, like hit numbers in gacha games. */
object DamageNumberPopups {
    /** Entities farther than this from the player do not get numbers. */
    private const val RANGE = 32.0

    /** The oldest numbers go first past this many. */
    private const val MAX_POPUPS = 64

    private val file: Path get() = FabricLoader.getInstance().configDir.resolve(DamageNumberStyle.FILE_NAME)

    var style = DamageNumberStyle()
        private set

    private class Popup(val entityId: Int, val x: Double, var y: Double, val z: Double, var damage: Float, var crit: Boolean, val killed: Boolean, val seed: Int) {
        /** Ticks since the number last popped, which a combo hit restarts. */
        var age = 0
        val intensity get() = DamageNumbers.intensity(damage, crit)
    }

    private val health = DamageNumbers.HealthTracker()
    private val crits = DamageNumbers.Marks()
    private val kills = DamageNumbers.Marks()
    private val popups = ArrayList<Popup>()
    private var level: ClientLevel? = null

    fun register() {
        load()
        ClientTickEvents.END_CLIENT_TICK.register(::tick)
        WorldRenderEvents.BEFORE_ENTITIES.register(::render)
    }

    fun load() {
        style = try {
            if (Files.exists(file)) DamageNumberStyle.parse(Files.readString(file)) else DamageNumberStyle()
        } catch (e: Exception) {
            McTinyTweak.logger.warn("Could not read {}, using defaults", file, e)
            DamageNumberStyle()
        }
    }

    fun update(change: (DamageNumberStyle) -> DamageNumberStyle) {
        style = change(style)
        try {
            Files.createDirectories(file.parent)
            Files.writeString(file, style.serialize())
        } catch (e: Exception) {
            McTinyTweak.logger.warn("Could not save {}", file, e)
        }
    }

    /** Called when the server shows a critical hit on [entityId], just before its health update arrives. */
    @JvmStatic
    fun critical(entityId: Int) {
        val time = Minecraft.getInstance().level?.gameTime ?: return
        crits.mark(entityId, time)
    }

    /** Called when [entityId] takes `/kill` damage, so its last number shows NULL instead. */
    @JvmStatic
    fun killed(entityId: Int) {
        val time = Minecraft.getInstance().level?.gameTime ?: return
        kills.mark(entityId, time)
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
        val time = level.gameTime
        for (entity in level.entitiesForRendering()) {
            if (entity !is LivingEntity || entity === player || entity.distanceToSqr(player) > RANGE * RANGE) continue
            seen += entity.id
            val lost = health.update(entity.id, entity.health) ?: continue
            hit(level, entity, lost, crits.take(entity.id, time), kills.take(entity.id, time))
        }
        health.retain(seen)
        crits.prune(time)
        kills.prune(time)
    }

    private fun hit(level: ClientLevel, entity: LivingEntity, damage: Float, crit: Boolean, killed: Boolean) {
        val combo = popups.lastOrNull { it.entityId == entity.id && !it.killed }
            ?.takeIf { !killed && style.combo && DamageNumbers.continuesCombo(it.age) }
        val popup = if (combo != null) {
            val wasBig = DamageNumbers.isBig(combo.intensity, combo.crit)
            // Keep the number where it has risen to, then pop it again with the new total.
            combo.y += DamageNumbers.rise(combo.age.toFloat())
            combo.damage += damage
            combo.crit = combo.crit || crit
            combo.age = 0
            if (wasBig && !crit) return
            combo
        } else {
            val random = entity.random
            Popup(
                entity.id,
                entity.x + (random.nextDouble() - 0.5) * entity.bbWidth,
                entity.y + entity.bbHeight + 0.2,
                entity.z + (random.nextDouble() - 0.5) * entity.bbWidth,
                damage,
                crit,
                killed,
                random.nextInt(),
            ).also {
                popups += it
                while (popups.size > MAX_POPUPS) popups.removeFirst()
            }
        }
        if (style.burst && (popup.killed || DamageNumbers.isBig(popup.intensity, popup.crit))) burst(level, popup)
    }

    /** Sparks flying out from the number, only on this client. */
    private fun burst(level: ClientLevel, popup: Popup) {
        val random = level.random
        val count = if (popup.killed) 28 else DamageNumbers.sparks(popup.intensity, popup.crit)
        repeat(count) {
            // An even spread over a sphere.
            val vy = random.nextDouble() * 2 - 1
            val angle = random.nextDouble() * Math.PI * 2
            val flat = sqrt(1 - vy * vy)
            val speed = 0.15 + random.nextDouble() * 0.2
            val type: ParticleOptions = when {
                popup.killed -> if (it % 2 == 0) ParticleTypes.REVERSE_PORTAL else ParticleTypes.SQUID_INK
                popup.crit -> ParticleTypes.CRIT
                else -> ParticleTypes.FIREWORK
            }
            level.addParticle(type, popup.x, popup.y + 0.2, popup.z, cos(angle) * flat * speed, vy * speed, sin(angle) * flat * speed)
        }
    }

    private fun reset() {
        popups.clear()
        health.clear()
        crits.clear()
        kills.clear()
    }

    private fun render(context: WorldRenderContext) {
        if (popups.isEmpty()) return
        val minecraft = Minecraft.getInstance()
        val font = minecraft.font
        val camera = context.worldState().cameraRenderState
        val partial = minecraft.deltaTracker.getGameTimeDeltaPartialTick(false)
        val poseStack = context.matrices()
        val queue = context.commandQueue()
        for (popup in popups) {
            val age = popup.age + partial
            val alpha = DamageNumbers.alpha(age)
            if (alpha <= 0f) continue
            val intensity = if (popup.killed) 1f else popup.intensity
            val big = popup.killed || DamageNumbers.isBig(intensity, popup.crit)

            poseStack.pushPose()
            poseStack.translate(popup.x - camera.pos.x, popup.y + DamageNumbers.rise(age) - camera.pos.y, popup.z - camera.pos.z)
            poseStack.mulPose(camera.orientation)
            val scale = 0.025f * DamageNumbers.scale(age, intensity, style.slam) * if (popup.killed) 1.4f else 1f
            poseStack.scale(scale, -scale, scale)

            var dx = 0f
            var dy = 0f
            if (style.shake && big) {
                dx += DamageNumbers.shakeX(age, intensity, popup.seed.toFloat())
                dy += DamageNumbers.shakeY(age, intensity, popup.seed.toFloat())
            }
            if (popup.killed) {
                drawNull(font, queue, poseStack, popup, age, alpha, dx, dy)
            } else {
                val text = number(popup, age, big)
                val x = -font.width(text) / 2f + dx
                val outline = DamageNumbers.withAlpha(ARGB.scaleRGB(DamageNumbers.color(popup.damage, popup.crit), 0.25f), alpha)
                queue.submitText(poseStack, x, dy, text, false, Font.DisplayMode.NORMAL, LightTexture.FULL_BRIGHT, DamageNumbers.withAlpha(DamageNumbers.WHITE, alpha), 0, outline)
            }
            poseStack.popPose()
        }
    }

    /** The number with each digit colored: flat, or flowing for huge hits, and flashing white when big hits land. */
    private fun number(popup: Popup, age: Float, big: Boolean): FormattedCharSequence {
        val value = if (style.countUp) DamageNumbers.countUp(popup.damage, age) else popup.damage
        val label = DamageNumbers.label(value, popup.crit, style.critMark)
        val gradient = style.gradient && DamageNumbers.isHuge(popup.intensity, popup.crit)
        val flash = if (style.flash && big) DamageNumbers.flash(age) else 0f
        val text = Component.empty()
        label.forEachIndexed { i, c ->
            var color = if (gradient) DamageNumbers.gradientColor(popup.damage, popup.crit, i, age) else DamageNumbers.color(popup.damage, popup.crit)
            if (flash > 0f) color = DamageNumbers.mix(color, DamageNumbers.WHITE, flash)
            text.append(Component.literal(c.toString()).withStyle(Style.EMPTY.withBold(true).withColor(TextColor.fromRgb(color and 0xFFFFFF))))
        }
        return text.visualOrderText
    }

    /** A glitching NULL: red and cyan ghosts split apart behind it, letters flicker and it jolts sideways. */
    private fun drawNull(
        font: Font,
        queue: net.minecraft.client.renderer.SubmitNodeCollector,
        poseStack: com.mojang.blaze3d.vertex.PoseStack,
        popup: Popup,
        age: Float,
        alpha: Float,
        shakeX: Float,
        shakeY: Float,
    ) {
        val tick = age.toInt()
        val text = Component.literal(DamageNumbers.glitchText(tick, popup.seed)).withStyle(Style.EMPTY.withBold(true)).visualOrderText
        val x = -font.width(text) / 2f + shakeX + DamageNumbers.glitchJolt(tick, popup.seed)
        val split = DamageNumbers.glitchSplit(tick, popup.seed)
        val light = LightTexture.FULL_BRIGHT
        val ghost = alpha * 0.85f
        queue.submitText(poseStack, x - split, shakeY, text, false, Font.DisplayMode.NORMAL, light, DamageNumbers.withAlpha(DamageNumbers.GLITCH_RED, ghost), 0, 0)
        queue.submitText(poseStack, x + split, shakeY, text, false, Font.DisplayMode.NORMAL, light, DamageNumbers.withAlpha(DamageNumbers.GLITCH_CYAN, ghost), 0, 0)
        queue.submitText(poseStack, x, shakeY, text, false, Font.DisplayMode.NORMAL, light, DamageNumbers.withAlpha(DamageNumbers.WHITE, alpha), 0, DamageNumbers.withAlpha(0xFF000000.toInt(), alpha))
    }
}
