package dev.luniiya.mctinytweak.client.damagenumbers

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.client.tweaks.Tweaks
import dev.luniiya.mctinytweak.damagenumbers.DamageNumberStyle
import dev.luniiya.mctinytweak.damagenumbers.DamageNumbers
import dev.luniiya.mctinytweak.damagenumbers.DamageVfx
import dev.luniiya.mctinytweak.healthbar.HealthBars
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
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

    /** Blocks per text pixel, as for name tags. */
    private const val PIXEL = 0.025f

    private val FONT = FontDescription.Resource(Identifier.fromNamespaceAndPath(McTinyTweak.MOD_ID, "damage"))

    private val file: Path get() = FabricLoader.getInstance().configDir.resolve(DamageNumberStyle.FILE_NAME)

    var style = DamageNumberStyle()
        private set

    private class Popup(
        val entityId: Int,
        val x: Double,
        var y: Double,
        val z: Double,
        /** The middle of the mob, where slashes cut. */
        val centerY: Double,
        var damage: Float,
        var crit: Boolean,
        val killed: Boolean,
        val seed: Int,
    ) {
        /** Ticks since the number last popped, which a combo hit restarts. */
        var age = 0

        /** Ticks since the number reached its [tier], which drives the tier effects. */
        var tierAge = 0
        var tier = DamageVfx.NONE
        var lifetime = DamageNumbers.LIFETIME
        var maxTagPlayed = false
        var shatterPlayed = false

        /** True if the player could see the hit, so its sounds and screen effects are theirs to get. */
        var inView = true
        val intensity get() = DamageNumbers.intensity(damage, crit)
    }

    private val health = DamageNumbers.HealthTracker()
    private val crits = DamageNumbers.Marks()
    private val kills = DamageNumbers.Marks()
    private val attackers = DamageNumbers.Attackers()
    private val popups = ArrayList<Popup>()
    private var level: ClientLevel? = null

    fun register() {
        load()
        ClientTickEvents.END_CLIENT_TICK.register(::tick)
        WorldRenderEvents.BEFORE_ENTITIES.register(::render)
        DamageImpact.register()
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

    /** Called when the server says [entityId] was hurt, by [causeId] or -1 for no one. */
    @JvmStatic
    fun damaged(entityId: Int, causeId: Int) {
        val time = Minecraft.getInstance().level?.gameTime ?: return
        attackers.record(entityId, causeId, time)
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
        val time = level.gameTime
        DamageImpact.tick(time)
        popups.forEach {
            it.age++
            it.tierAge++
            delayedSounds(level, it)
        }
        popups.removeIf { it.age >= it.lifetime }

        val seen = HashSet<Int>()
        for (entity in level.entitiesForRendering()) {
            if (entity !is LivingEntity || entity === player || entity.distanceToSqr(player) > RANGE * RANGE) continue
            seen += entity.id
            val lost = health.update(entity.id, entity.health) ?: continue
            val crit = crits.take(entity.id, time)
            val killed = kills.take(entity.id, time)
            if (!DamageNumbers.shows(style.onlyMine, attackers.of(entity.id, time), player.id)) continue
            hit(minecraft, level, entity, lost, crit, killed)
        }
        health.retain(seen)
        crits.prune(time)
        kills.prune(time)
        attackers.prune(time)
    }

    private fun hit(minecraft: Minecraft, level: ClientLevel, entity: LivingEntity, damage: Float, crit: Boolean, killed: Boolean) {
        val combo = popups.lastOrNull { it.entityId == entity.id && !it.killed }
            ?.takeIf { !killed && style.combo && DamageNumbers.continuesCombo(it.age) }
        val popup: Popup
        var burst = true
        if (combo != null) {
            val wasBig = DamageNumbers.isBig(combo.intensity, combo.crit)
            // Keep the number where it has risen to, then pop it again with the new total.
            combo.y += DamageNumbers.rise(combo.age.toFloat(), combo.lifetime)
            combo.damage += damage
            combo.crit = combo.crit || crit
            combo.age = 0
            combo.shatterPlayed = false
            if (wasBig && !crit) burst = false
            popup = combo
        } else {
            val random = entity.random
            popup = Popup(
                entity.id,
                entity.x + (random.nextDouble() - 0.5) * entity.bbWidth,
                entity.y + entity.bbHeight + 0.2,
                entity.z + (random.nextDouble() - 0.5) * entity.bbWidth,
                entity.y + entity.bbHeight * 0.5,
                damage,
                crit,
                killed,
                random.nextInt(),
            )
            popups += popup
            while (popups.size > MAX_POPUPS) popups.removeFirst()
        }
        if (style.burst && burst && (popup.killed || DamageNumbers.isBig(popup.intensity, popup.crit))) burst(level, popup)

        // A combo climbing into a new tier sets off that tier's effects again.
        val tier = if (popup.killed) DamageVfx.NONE else DamageVfx.tier(popup.damage)
        if (tier > popup.tier) {
            popup.inView = inView(minecraft, popup)
            popup.tier = tier
            popup.tierAge = 0
            popup.maxTagPlayed = false
            tierUp(level, popup)
        }
        popup.lifetime = DamageVfx.lifetime(popup.tier)
    }

    /** True if the number's spot is on screen, so hits behind the player do not boom and flash at them. */
    private fun inView(minecraft: Minecraft, popup: Popup): Boolean {
        val camera = minecraft.gameRenderer.mainCamera
        val forward = camera.forwardVector()
        val window = minecraft.window
        val viewCos = DamageNumbers.viewCos(minecraft.options.fov().get().toDouble(), window.width.toDouble() / maxOf(window.height, 1))
        val eye = camera.position()
        return DamageNumbers.inView(
            popup.x - eye.x, popup.y - eye.y, popup.z - eye.z,
            forward.x().toDouble(), forward.y().toDouble(), forward.z().toDouble(),
            viewCos,
        )
    }

    private fun tierUp(level: ClientLevel, popup: Popup) {
        val time = level.gameTime
        if (style.burst) tierParticles(level, popup)
        if (!popup.inView) return
        if (style.sounds) tierSound(level, popup.tier)
        if (style.focusLines && popup.tier >= DamageVfx.DEVASTATING) DamageImpact.focus(time, popup.tier, popup.seed)
        if (style.impactFrame && popup.tier >= DamageVfx.MAX) DamageImpact.impact(time)
    }

    /** Sounds only this player hears, played right at them so they hit hard wherever the mob is. */
    private fun play(level: ClientLevel, sound: SoundEvent, volume: Float, pitch: Float) {
        val player = Minecraft.getInstance().player ?: return
        level.playLocalSound(player.x, player.eyeY, player.z, sound, SoundSource.PLAYERS, volume, pitch, false)
    }

    private fun tierSound(level: ClientLevel, tier: Int) {
        when (tier) {
            DamageVfx.HEAVY -> {
                play(level, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 1.2f, 0.5f)
                play(level, SoundEvents.PLAYER_ATTACK_CRIT, 0.8f, 0.6f)
            }
            DamageVfx.BRUTAL -> {
                play(level, SoundEvents.MACE_SMASH_GROUND_HEAVY, 0.9f, 0.9f)
                play(level, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 1.2f, 0.5f)
            }
            DamageVfx.DEVASTATING -> {
                play(level, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1f, 0.7f)
                play(level, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.6f, 0.6f)
                play(level, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.9f, 0.7f)
            }
            DamageVfx.MAX -> {
                play(level, SoundEvents.WARDEN_SONIC_BOOM, 1f, 0.8f)
                play(level, SoundEvents.BELL_BLOCK, 1f, 0.5f)
                play(level, SoundEvents.ANVIL_LAND, 0.5f, 0.5f)
                play(level, SoundEvents.TRIDENT_THUNDER.value(), 0.5f, 1.4f)
            }
        }
    }

    /** The MAX tag's sparkle and the glass breaking when a number shatters, timed to the animation. */
    private fun delayedSounds(level: ClientLevel, popup: Popup) {
        if (!style.sounds || !popup.inView) return
        if (style.rainbow && popup.tier >= DamageVfx.MAX && !popup.maxTagPlayed && popup.tierAge >= DamageVfx.MAX_TAG_DELAY) {
            popup.maxTagPlayed = true
            play(level, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1f, 1.2f)
            play(level, SoundEvents.TOTEM_USE, 0.35f, 1.6f)
        }
        if (style.shatter && popup.tier >= DamageVfx.DEVASTATING && !popup.shatterPlayed &&
            DamageVfx.shatter(popup.age.toFloat(), popup.lifetime) > 0f
        ) {
            popup.shatterPlayed = true
            play(level, SoundEvents.GLASS_BREAK, 1f, 0.8f)
            play(level, SoundEvents.AMETHYST_CLUSTER_BREAK, 1f, 0.7f)
        }
    }

    /** Sparks flying out from the number, only on this client. */
    private fun burst(level: ClientLevel, popup: Popup) {
        val count = if (popup.killed) 28 else DamageNumbers.sparks(popup.intensity, popup.crit)
        sphere(level, popup.x, popup.y + 0.2, popup.z, count, 0.15, 0.2) {
            when {
                popup.killed -> if (it % 2 == 0) ParticleTypes.REVERSE_PORTAL else ParticleTypes.SQUID_INK
                popup.crit -> ParticleTypes.CRIT
                else -> ParticleTypes.FIREWORK
            }
        }
    }

    /** Heart-colored dust from 200 up, glowing rods and electric sparks from 500 up, totem confetti at MAX. */
    private fun tierParticles(level: ClientLevel, popup: Popup) {
        val color = DamageNumbers.color(popup.damage) and 0xFFFFFF
        if (popup.tier >= DamageVfx.BRUTAL) {
            val dust = DustParticleOptions(color, 1.6f)
            val gold = DustParticleOptions(HealthBars.GOLD and 0xFFFFFF, 1.2f)
            sphere(level, popup.x, popup.centerY, popup.z, 40, 0.2, 0.25) { if (it % 3 == 0) gold else dust }
        }
        if (popup.tier >= DamageVfx.DEVASTATING) {
            sphere(level, popup.x, popup.y + 0.2, popup.z, 30, 0.3, 0.3) { if (it % 2 == 0) ParticleTypes.END_ROD else ParticleTypes.ELECTRIC_SPARK }
        }
        if (popup.tier >= DamageVfx.MAX) {
            sphere(level, popup.x, popup.y + 0.2, popup.z, 90, 0.4, 0.5) { ParticleTypes.TOTEM_OF_UNDYING }
            val rainbow = DamageVfx.RAINBOW.map { DustParticleOptions(it and 0xFFFFFF, 2f) }
            sphere(level, popup.x, popup.centerY, popup.z, 60, 0.3, 0.3) { rainbow[it % rainbow.size] }
        }
    }

    /** [count] particles thrown out evenly over a sphere at [speed] plus up to [spread] more. */
    private inline fun sphere(level: ClientLevel, x: Double, y: Double, z: Double, count: Int, speed: Double, spread: Double, type: (Int) -> ParticleOptions) {
        val random = level.random
        repeat(count) {
            val vy = random.nextDouble() * 2 - 1
            val angle = random.nextDouble() * Math.PI * 2
            val flat = sqrt(1 - vy * vy)
            val s = speed + random.nextDouble() * spread
            level.addParticle(type(it), x, y, z, cos(angle) * flat * s, vy * s, sin(angle) * flat * s)
        }
    }

    private fun reset() {
        popups.clear()
        health.clear()
        crits.clear()
        kills.clear()
        attackers.clear()
        DamageImpact.reset()
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
            val tierAge = popup.tierAge + partial
            val alpha = DamageNumbers.alpha(age, popup.lifetime)
            if (alpha <= 0f) continue
            val intensity = if (popup.killed) 1f else popup.intensity
            val big = popup.killed || DamageNumbers.isBig(intensity, popup.crit)

            if (style.slash && popup.tier >= DamageVfx.HEAVY) {
                billboard(poseStack, camera, popup.x, popup.centerY, popup.z)
                slashes(queue, poseStack, popup, tierAge)
                poseStack.popPose()
            }

            val size = DamageNumbers.scale(age, intensity, style.slam) * if (popup.killed) 1.4f else 1f
            billboard(poseStack, camera, popup.x, popup.y + DamageNumbers.rise(age, popup.lifetime), popup.z)
            // Glows are centered on the digits and sit a little behind them so they never wash over them.
            poseStack.pushPose()
            poseStack.translate(0f, 4f * size, -1f)
            if (style.rays && popup.tier >= DamageVfx.DEVASTATING) rays(queue, poseStack, popup, tierAge, alpha)
            if (style.rainbow && popup.tier >= DamageVfx.MAX) starburst(queue, poseStack, tierAge)
            if (style.rings && popup.tier >= DamageVfx.HEAVY) rings(queue, poseStack, popup, tierAge)
            poseStack.popPose()

            var dx = 0f
            var dy = 0f
            if (style.shake && big) {
                dx += DamageNumbers.shakeX(age, intensity, popup.seed.toFloat())
                dy += DamageNumbers.shakeY(age, intensity, popup.seed.toFloat())
            }
            if (popup.killed) {
                poseStack.scale(size, size, size)
                drawNull(font, queue, poseStack, popup, age, alpha, dx, dy)
            } else {
                if (style.afterimages && popup.tier >= DamageVfx.BRUTAL) afterimages(font, queue, poseStack, popup, age, intensity)
                poseStack.pushPose()
                poseStack.scale(size, size, size)
                val shatter = DamageVfx.shatter(age, popup.lifetime)
                if (style.shatter && popup.tier >= DamageVfx.DEVASTATING && shatter > 0f) {
                    shattered(font, queue, poseStack, popup, age, shatter, dx, dy)
                } else {
                    val text = number(popup, age, big, flat = null)
                    val x = -font.width(text) / 2f + dx
                    queue.submitText(poseStack, x, dy, text, false, Font.DisplayMode.NORMAL, LightTexture.FULL_BRIGHT, DamageNumbers.withAlpha(DamageNumbers.WHITE, alpha), 0, outline(popup, alpha))
                }
                if (style.rainbow && popup.tier >= DamageVfx.MAX) maxTag(font, queue, poseStack, popup, tierAge, alpha)
                poseStack.popPose()
            }
            poseStack.popPose()
        }
    }

    /**
     * Pushes a pose facing the camera at a world spot, in text pixels with y pointing down. The spot is pulled
     * toward the camera, and shrunk to match, so flames and the mob's body cannot cover what is drawn there.
     */
    private fun billboard(poseStack: PoseStack, camera: net.minecraft.client.renderer.state.CameraRenderState, x: Double, y: Double, z: Double) {
        poseStack.pushPose()
        val rx = x - camera.pos.x
        val ry = y - camera.pos.y
        val rz = z - camera.pos.z
        val pull = DamageNumbers.pull(sqrt(rx * rx + ry * ry + rz * rz))
        poseStack.translate(rx * pull, ry * pull, rz * pull)
        poseStack.mulPose(camera.orientation)
        val scale = PIXEL * pull.toFloat()
        poseStack.scale(scale, -scale, scale)
    }

    private fun outline(popup: Popup, alpha: Float): Int {
        val color = if (style.rainbow && popup.tier >= DamageVfx.MAX) DamageNumbers.BLACK else DamageNumbers.color(popup.damage)
        return DamageNumbers.withAlpha(DamageNumbers.outline(color, popup.crit), alpha)
    }

    private fun textStyle(color: Int): Style {
        var s = Style.EMPTY.withBold(!style.font).withColor(TextColor.fromRgb(color and 0xFFFFFF))
        if (style.font) s = s.withFont(FONT)
        return s
    }

    /** The color of character [index]: rainbow at MAX, flowing for huge hits, else the heart color. */
    private fun digitColor(popup: Popup, index: Int, age: Float): Int = when {
        style.rainbow && popup.tier >= DamageVfx.MAX -> DamageVfx.rainbow(index, age)
        style.gradient && DamageNumbers.isHuge(popup.intensity, popup.crit) -> DamageNumbers.gradientColor(popup.damage, index, age)
        else -> DamageNumbers.color(popup.damage)
    }

    private fun label(popup: Popup, age: Float): String {
        val value = if (style.countUp) DamageNumbers.countUp(popup.damage, age) else popup.damage
        return DamageNumbers.label(value, popup.crit, style.critMark)
    }

    /**
     * The number with each digit colored and flashing white when big hits land, or all in [flat] for
     * afterimages.
     */
    private fun number(popup: Popup, age: Float, big: Boolean, flat: Int?): FormattedCharSequence {
        val flash = if (style.flash && big) DamageNumbers.flash(age) else 0f
        val text = Component.empty()
        label(popup, age).forEachIndexed { i, c ->
            text.append(Component.literal(c.toString()).withStyle(textStyle(flat ?: charColor(popup, i, age, flash))))
        }
        return text.visualOrderText
    }

    private fun charColor(popup: Popup, index: Int, age: Float, flash: Float): Int {
        val color = DamageNumbers.fill(digitColor(popup, index, age), popup.crit)
        return if (flash > 0f) DamageNumbers.mix(color, DamageNumbers.WHITE, flash) else color
    }

    /** Glowing copies of the number where it was a moment ago, bigger and lower, trailing behind it. */
    private fun afterimages(font: Font, queue: SubmitNodeCollector, poseStack: PoseStack, popup: Popup, age: Float, intensity: Float) {
        val count = DamageVfx.afterimages(popup.tier)
        for (k in count downTo 1) {
            val a = DamageVfx.afterimageAlpha(age, k, count)
            val then = age - k * DamageVfx.AFTERIMAGE_GAP
            if (a <= 0f || then < 0f) continue
            val color = if (style.rainbow && popup.tier >= DamageVfx.MAX) DamageVfx.rainbow(k, age) else DamageNumbers.color(popup.damage)
            val text = number(popup, age, big = false, flat = color)
            val size = DamageNumbers.scale(then, intensity, style.slam)
            // How much lower the number was then, in text pixels.
            val drop = (DamageNumbers.rise(age, popup.lifetime) - DamageNumbers.rise(then, popup.lifetime)) / PIXEL
            poseStack.pushPose()
            poseStack.translate(0f, drop, -0.4f * k)
            poseStack.scale(size, size, size)
            queue.submitText(poseStack, -font.width(text) / 2f, 0f, text, false, Font.DisplayMode.NORMAL, LightTexture.FULL_BRIGHT, DamageNumbers.withAlpha(DamageNumbers.WHITE, a), 0, 0)
            poseStack.popPose()
        }
    }

    /** The number breaking apart: each character flies off spinning and falls, with glowing shards. */
    private fun shattered(font: Font, queue: SubmitNodeCollector, poseStack: PoseStack, popup: Popup, age: Float, progress: Float, dx: Float, dy: Float) {
        val label = label(popup, age)
        val fade = 1f - progress * progress
        val outline = outline(popup, fade)
        var x = -font.width(number(popup, age, big = false, flat = DamageNumbers.WHITE)) / 2f + dx
        label.forEachIndexed { i, c ->
            val char = Component.literal(c.toString()).withStyle(textStyle(charColor(popup, i, age, 0f))).visualOrderText
            val w = font.width(char).toFloat()
            val (sx, sy, spin) = DamageVfx.shard(i, progress, popup.seed)
            poseStack.pushPose()
            poseStack.translate(x + w / 2 + sx, dy + 4 + sy, 0f)
            poseStack.mulPose(Axis.ZP.rotation(spin))
            queue.submitText(poseStack, -w / 2, -4f, char, false, Font.DisplayMode.NORMAL, LightTexture.FULL_BRIGHT, DamageNumbers.withAlpha(DamageNumbers.WHITE, fade), 0, outline)
            poseStack.popPose()
            x += w
        }
        val color = DamageNumbers.color(popup.damage)
        glow(queue, poseStack) { pose, v ->
            for (i in 0 until 14) {
                val (sx, sy, spin) = DamageVfx.shard(i + 100, progress, popup.seed)
                val r = 3.5f * (1f - progress) + 0.5f
                val c = DamageNumbers.withAlpha(if (i % 2 == 0) color else DamageNumbers.WHITE, fade)
                val ox = sx * 1.3f + dx
                val oy = sy * 1.3f + dy + 4
                tri(v, pose, ox + cos(spin) * r, oy + sin(spin) * r, c, ox + cos(spin + 2.2f) * r, oy + sin(spin + 2.2f) * r, c, ox + cos(spin + 4.1f) * r * 0.6f, oy + sin(spin + 4.1f) * r * 0.6f, c)
            }
        }
    }

    /** A bouncing rainbow MAX above the number. */
    private fun maxTag(font: Font, queue: SubmitNodeCollector, poseStack: PoseStack, popup: Popup, tierAge: Float, alpha: Float) {
        val scale = DamageVfx.maxTagScale(tierAge)
        if (scale <= 0f) return
        val text = Component.empty()
        "MAX".forEachIndexed { i, c ->
            text.append(Component.literal(c.toString()).withStyle(textStyle(DamageVfx.rainbow(i + 3, tierAge * 1.5f))))
        }
        val seq = text.visualOrderText
        poseStack.pushPose()
        poseStack.translate(0f, -7f, 0f)
        poseStack.mulPose(Axis.ZP.rotation(-0.12f + 0.05f * sin(tierAge * 0.8f)))
        poseStack.scale(scale * 0.75f, scale * 0.75f, scale * 0.75f)
        queue.submitText(poseStack, -font.width(seq) / 2f, -9f, seq, false, Font.DisplayMode.NORMAL, LightTexture.FULL_BRIGHT, DamageNumbers.withAlpha(DamageNumbers.WHITE, alpha), 0, DamageNumbers.withAlpha(DamageNumbers.WHITE, alpha))
        poseStack.popPose()
    }

    /** Shockwave rings: glowing bands that fly out from the number and thin as they go. */
    private fun rings(queue: SubmitNodeCollector, poseStack: PoseStack, popup: Popup, tierAge: Float) {
        glow(queue, poseStack) { pose, v ->
            for (index in 0 until DamageVfx.rings(popup.tier)) {
                val radius = DamageVfx.ringRadius(tierAge, index, popup.tier) ?: continue
                val width = DamageVfx.ringWidth(tierAge, index)
                val a = DamageVfx.ringAlpha(tierAge, index)
                val base = when {
                    style.rainbow && popup.tier >= DamageVfx.MAX -> DamageVfx.rainbow(index * 2, tierAge)
                    index % 2 == 1 -> DamageNumbers.WHITE
                    else -> DamageNumbers.color(popup.damage)
                }
                val mid = DamageNumbers.withAlpha(base, a)
                val clear = DamageNumbers.withAlpha(base, 0f)
                val segments = 48
                for (s in 0 until segments) {
                    val a0 = (s * 2 * Math.PI / segments).toFloat()
                    val a1 = ((s + 1) * 2 * Math.PI / segments).toFloat()
                    for ((from, to) in listOf(radius - width to radius, radius + width to radius)) {
                        val f = maxOf(from, 0f)
                        quad(
                            v, pose,
                            cos(a0) * f, sin(a0) * f, clear,
                            cos(a1) * f, sin(a1) * f, clear,
                            cos(a1) * to, sin(a1) * to, mid,
                            cos(a0) * to, sin(a0) * to, mid,
                        )
                    }
                }
            }
        }
    }

    /** Rays of light spinning behind the number, two layers turning opposite ways at MAX. */
    private fun rays(queue: SubmitNodeCollector, poseStack: PoseStack, popup: Popup, tierAge: Float, alpha: Float) {
        val count = DamageVfx.rays(popup.tier)
        val length = DamageVfx.rayLength(tierAge, popup.tier)
        val layers = if (popup.tier >= DamageVfx.MAX) 2 else 1
        val strength = 0.75f * alpha
        glow(queue, poseStack) { pose, v ->
            for (layer in 0 until layers) {
                for (i in 0 until count) {
                    val angle = DamageVfx.rayAngle(i, count, tierAge, layer)
                    val color = when {
                        style.rainbow && popup.tier >= DamageVfx.MAX -> DamageVfx.rainbow(i, tierAge)
                        i % 2 == 0 -> DamageNumbers.color(popup.damage)
                        else -> HealthBars.GOLD
                    }
                    val len = length * (if (i % 2 == 0) 1f else 0.7f) * (if (layer == 1) 0.75f else 1f)
                    val half = 0.07f
                    tri(
                        v, pose,
                        0f, 0f, DamageNumbers.withAlpha(DamageNumbers.mix(color, DamageNumbers.WHITE, 0.5f), strength),
                        cos(angle - half) * len, sin(angle - half) * len, DamageNumbers.withAlpha(color, 0f),
                        cos(angle + half) * len, sin(angle + half) * len, DamageNumbers.withAlpha(color, 0f),
                    )
                }
            }
        }
    }

    /** A four-pointed star flaring white and gold, then collapsing to a point. */
    private fun starburst(queue: SubmitNodeCollector, poseStack: PoseStack, tierAge: Float) {
        val size = DamageVfx.starScale(tierAge)
        if (size <= 0f) return
        glow(queue, poseStack) { pose, v ->
            for ((points, length, color) in listOf(Triple(4, 110f, DamageNumbers.WHITE), Triple(4, 70f, HealthBars.GOLD), Triple(8, 40f, DamageNumbers.WHITE))) {
                val turn = tierAge * 0.05f + if (color == HealthBars.GOLD) (Math.PI / 4).toFloat() else 0f
                for (p in 0 until points) {
                    val angle = (p * 2 * Math.PI / points).toFloat() + turn
                    val len = length * size
                    val waist = 6f * size
                    val core = DamageNumbers.withAlpha(color, 1f)
                    val tip = DamageNumbers.withAlpha(color, 0f)
                    tri(
                        v, pose,
                        cos(angle + 1.57f) * waist, sin(angle + 1.57f) * waist, core,
                        cos(angle) * len, sin(angle) * len, tip,
                        cos(angle - 1.57f) * waist, sin(angle - 1.57f) * waist, core,
                    )
                }
            }
        }
    }

    /** Slashes across the mob: a glowing heart-colored cut with a white-hot core, then an X, then a star. */
    private fun slashes(queue: SubmitNodeCollector, poseStack: PoseStack, popup: Popup, tierAge: Float) {
        val count = DamageVfx.slashes(popup.tier)
        val color = if (style.rainbow && popup.tier >= DamageVfx.MAX) null else DamageNumbers.color(popup.damage)
        glow(queue, poseStack) { pose, v ->
            for (i in 0 until count) {
                val length = DamageVfx.slashLength(tierAge, i)
                val width = DamageVfx.slashWidth(tierAge, i)
                if (length <= 0f || width <= 0f) continue
                val angle = DamageVfx.slashAngle(i, count, popup.seed)
                val reach = (38f + 6f * popup.tier) * length
                val c = color ?: DamageVfx.rainbow(i * 2, tierAge)
                // It cuts from one end, so the tail stays put while the head flies out.
                val ax = -cos(angle) * reach
                val ay = -sin(angle) * reach
                val bx = cos(angle) * reach * (2 * length - 1)
                val by = sin(angle) * reach * (2 * length - 1)
                diamond(v, pose, ax, ay, bx, by, 6f * width, DamageNumbers.withAlpha(c, width))
                diamond(v, pose, ax, ay, bx, by, 2f * width, DamageNumbers.withAlpha(DamageNumbers.WHITE, width))
            }
        }
    }

    /** Draws additive glowing triangles: no depth writes, colors adding up like light. */
    private inline fun glow(queue: SubmitNodeCollector, poseStack: PoseStack, crossinline draw: (PoseStack.Pose, VertexConsumer) -> Unit) {
        queue.submitCustomGeometry(poseStack, RenderTypes.dragonRays()) { pose, consumer -> draw(pose, consumer) }
    }

    /** A long thin diamond from (ax, ay) to (bx, by), [half] wide in the middle. */
    private fun diamond(v: VertexConsumer, pose: PoseStack.Pose, ax: Float, ay: Float, bx: Float, by: Float, half: Float, color: Int) {
        val dx = bx - ax
        val dy = by - ay
        val len = sqrt(dx * dx + dy * dy)
        if (len <= 0f) return
        val nx = -dy / len * half
        val ny = dx / len * half
        val mx = (ax + bx) / 2
        val my = (ay + by) / 2
        val edge = DamageNumbers.withAlpha(color, 0f)
        tri(v, pose, ax, ay, edge, mx + nx, my + ny, color, mx - nx, my - ny, color)
        tri(v, pose, bx, by, edge, mx + nx, my + ny, color, mx - nx, my - ny, color)
    }

    private fun quad(v: VertexConsumer, pose: PoseStack.Pose, x1: Float, y1: Float, c1: Int, x2: Float, y2: Float, c2: Int, x3: Float, y3: Float, c3: Int, x4: Float, y4: Float, c4: Int) {
        tri(v, pose, x1, y1, c1, x2, y2, c2, x3, y3, c3)
        tri(v, pose, x1, y1, c1, x3, y3, c3, x4, y4, c4)
    }

    /** One triangle, sent both ways around so it shows whichever side faces the camera. */
    private fun tri(v: VertexConsumer, pose: PoseStack.Pose, x1: Float, y1: Float, c1: Int, x2: Float, y2: Float, c2: Int, x3: Float, y3: Float, c3: Int) {
        v.addVertex(pose, x1, y1, 0f).setColor(c1)
        v.addVertex(pose, x2, y2, 0f).setColor(c2)
        v.addVertex(pose, x3, y3, 0f).setColor(c3)
        v.addVertex(pose, x1, y1, 0f).setColor(c1)
        v.addVertex(pose, x3, y3, 0f).setColor(c3)
        v.addVertex(pose, x2, y2, 0f).setColor(c2)
    }

    /** A glitching NULL: red and cyan ghosts split apart behind it, letters flicker and it jolts sideways. */
    private fun drawNull(
        font: Font,
        queue: SubmitNodeCollector,
        poseStack: PoseStack,
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
