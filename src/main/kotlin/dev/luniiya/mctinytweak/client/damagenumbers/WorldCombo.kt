package dev.luniiya.mctinytweak.client.damagenumbers

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.damagenumbers.ComboCounter
import dev.luniiya.mctinytweak.damagenumbers.DamageNumbers
import dev.luniiya.mctinytweak.damagenumbers.DamageVfx
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.state.CameraRenderState
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.LivingEntity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The player's combo: their hits in a row. Like damage numbers, every hit spawns popups at random spots around
 * the mob, in the color of its damage number, that pop, rise and fade: "13 HITS" with a draining timer bar on
 * one side and the combo's DMG total across from it, plus the rank over its head when a new one is reached.
 */
object WorldCombo {
    val combo = ComboCounter()

    private val FONT = FontDescription.Resource(Identifier.fromNamespaceAndPath(McTinyTweak.MOD_ID, "damage"))

    /** Pixels the count is drawn at before its pop, from the font's own size. */
    private const val SIZE = 2.6f

    /** The popups' size against a name tag's pixels. */
    private const val SCALE = 0.8f

    /** Blocks between the mob's edge and the popups around it. */
    private const val GAP = 0.45

    /** Ticks each kind of popup lives: ranks stay a little longer. */
    private const val LIFETIME = 26
    private const val RANK_LIFETIME = 40

    /** The oldest popups go first past this many. */
    private const val MAX_POPUPS = 32

    private enum class Kind { COUNT, TOTAL, RANK }

    private class Popup(
        val kind: Kind,
        /** The mob's middle, which the popup bursts out from, and its spot around the mob. */
        val fromX: Double, val fromY: Double, val fromZ: Double,
        val x: Double, val y: Double, val z: Double,
        val clearance: Double,
        val count: Int,
        val total: Float,
        /** The damage number's heart color, and whether it was a crit, which flips its fill and outline. */
        val color: Int,
        val crit: Boolean,
        val rainbow: Boolean,
        /** Which popup of its hit it is, so they fly out one after another. */
        val order: Int,
    ) {
        var age = 0
        val lifetime = if (kind == Kind.RANK) RANK_LIFETIME else LIFETIME
    }

    private val popups = ArrayList<Popup>()

    /**
     * Counts one of the player's hits on [target], whose damage number shows in [color] (dark with a bright
     * outline for a [crit]) or rainbow, and spawns its popups, particles and sounds.
     */
    fun hit(time: Long, damage: Float, target: LivingEntity, color: Int, crit: Boolean, rainbow: Boolean) {
        val rankBefore = ComboCounter.rankLevel(combo.count)
        combo.hit(time, damage)
        val style = DamageNumberPopups.style
        val count = combo.count
        if (!style.comboCounter || !ComboCounter.shown(count)) return
        val rank = ComboCounter.rankLevel(count)
        val rankUp = rank > rankBefore
        spawn(target, count, color, crit, rainbow, rankUp)
        if (style.burst) particles(target, count, rank, rankUp, color)
        if (!style.sounds) return
        if (ComboCounter.milestone(count)) DamageSounds.play(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8f, 0.8f + 0.15f * minOf(rank, 8))
        if (rankUp) DamageSounds.play(SoundEvents.PLAYER_LEVELUP, 0.5f, 1f + 0.1f * rank)
    }

    /** The count on a random side of the mob, the DMG total across from it and a new rank over its head. */
    private fun spawn(target: LivingEntity, count: Int, color: Int, crit: Boolean, rainbow: Boolean, rankUp: Boolean) {
        val camera = Minecraft.getInstance().gameRenderer.mainCamera
        val left = camera.leftVector()
        val up = camera.upVector()
        val random = target.random
        val cx = target.x
        val cy = target.y + target.bbHeight * 0.5
        val cz = target.z
        val sideways = target.bbWidth / 2 + GAP
        val upward = target.bbHeight / 2 + GAP * 0.5
        val clearance = DamageNumbers.clearance(target.bbWidth.toDouble())
        fun add(kind: Kind, angle: Float, reach: Double, order: Int) {
            val (across, high) = ComboCounter.around(angle)
            // Camera left is negative across, so a positive angle lands on the right as seen.
            val dx = -left.x() * across * sideways * reach + up.x() * high * upward * reach
            val dy = -left.y() * across * sideways * reach + up.y() * high * upward * reach
            val dz = -left.z() * across * sideways * reach + up.z() * high * upward * reach
            popups += Popup(kind, cx, cy, cz, cx + dx, cy + dy, cz + dz, clearance, count, combo.total, color, crit, rainbow, order)
        }
        val angle = ComboCounter.sideAngle(random.nextFloat())
        add(Kind.COUNT, angle, 1.0 + random.nextDouble() * 0.35, 0)
        add(Kind.TOTAL, angle + PI.toFloat() + (random.nextFloat() - 0.5f) * 0.6f, 1.0 + random.nextDouble() * 0.35, 1)
        if (rankUp) add(Kind.RANK, (PI / 2).toFloat() + (random.nextFloat() - 0.5f) * 0.5f, 1.5, 2)
        while (popups.size > MAX_POPUPS) popups.removeFirst()
    }

    /**
     * Particles in the number's color around the mob on every tap, more the longer the combo: sparks from 10,
     * a big burst every 10th hit, confetti for each new rank and rainbow dust from 100.
     */
    private fun particles(target: LivingEntity, count: Int, rank: Int, rankUp: Boolean, color: Int) {
        val level = target.level() as? ClientLevel ?: return
        val x = target.x
        val y = target.y + target.bbHeight * 0.5
        val z = target.z
        val clearance = DamageNumbers.clearance(target.bbWidth.toDouble())
        val spread = 0.12 + 0.03 * rank
        val dust = DustParticleOptions(color and 0xFFFFFF, 1.2f + 0.15f * rank)
        val white = DustParticleOptions(0xFFFFFF, 0.9f)
        DamageNumberPopups.sphere(level, x, y, z, clearance, 8 + 3 * rank + minOf(count, 60) / 3, spread, spread) { if (it % 4 == 0) white else dust }
        if (count >= ComboCounter.FLASH_FROM) {
            DamageNumberPopups.sphere(level, x, y, z, clearance, 4 + 2 * rank, 0.25, 0.2) { if (it % 2 == 0) ParticleTypes.ELECTRIC_SPARK else ParticleTypes.CRIT }
        }
        if (ComboCounter.milestone(count)) {
            DamageNumberPopups.sphere(level, x, y, z, clearance, 50 + 10 * rank, 0.3, 0.3) { if (it % 3 == 0) white else dust }
            DamageNumberPopups.sphere(level, x, y, z, clearance, 25 + 5 * rank, 0.35, 0.25) { if (it % 2 == 0) ParticleTypes.END_ROD else ParticleTypes.FIREWORK }
        }
        if (rankUp) {
            DamageNumberPopups.sphere(level, x, y + target.bbHeight * 0.5, z, clearance, 40 + 15 * rank, 0.4, 0.4) { ParticleTypes.TOTEM_OF_UNDYING }
        }
        if (ComboCounter.rainbow(count)) {
            val rainbow = DamageVfx.RAINBOW.map { DustParticleOptions(it and 0xFFFFFF, 1.6f) }
            DamageNumberPopups.sphere(level, x, y, z, clearance, 20, 0.25, 0.25) { rainbow[it % rainbow.size] }
        }
    }

    fun tick() {
        popups.forEach { it.age++ }
        popups.removeIf { it.age >= it.lifetime }
    }

    fun clear() {
        combo.clear()
        popups.clear()
    }

    /** True while there are popups to draw. */
    fun visible(): Boolean = DamageNumberPopups.style.comboCounter && popups.isNotEmpty()

    fun render(font: Font, queue: SubmitNodeCollector, poseStack: PoseStack, camera: CameraRenderState, gameTime: Long, partial: Float) {
        if (!visible()) return
        val time = gameTime + partial
        val chunky = DamageNumberPopups.style.font
        for (popup in popups) {
            val age = popup.age + partial
            val alpha = DamageNumbers.alpha(age, popup.lifetime)
            if (alpha <= 0f) continue
            val color = if (popup.rainbow) DamageVfx.rainbow(popup.order, time * 0.6f) else popup.color
            // Drawn just like the damage number: its fill and outline, black around rainbow.
            val fill = if (popup.rainbow) color else DamageNumbers.fill(color, popup.crit)
            val outline = DamageNumbers.withAlpha(if (popup.rainbow) DamageNumbers.BLACK else DamageNumbers.outline(color, popup.crit), alpha)
            // Bursting out from the mob's middle to its spot, then rising like a damage number.
            val out = ComboCounter.spread(age, popup.order).toDouble()
            val x = popup.fromX + (popup.x - popup.fromX) * out
            val y = popup.fromY + (popup.y - popup.fromY) * out + DamageNumbers.rise(age, popup.lifetime)
            val z = popup.fromZ + (popup.z - popup.fromZ) * out
            DamageNumberPopups.billboard(poseStack, camera, x, y, z, popup.clearance)
            poseStack.scale(SCALE, SCALE, SCALE)
            when (popup.kind) {
                Kind.COUNT -> count(font, queue, poseStack, popup, age, time, color, fill, outline, chunky, alpha)
                Kind.TOTAL -> total(font, queue, poseStack, age, popup, fill, outline, chunky, alpha)
                Kind.RANK -> rank(font, queue, poseStack, popup, age, time, fill, outline, chunky, alpha)
            }
            poseStack.popPose()
        }
    }

    /** The count, its echo, flash, burst and sparkles, with HITS and the timer bar under it, centered. */
    private fun count(font: Font, queue: SubmitNodeCollector, poseStack: PoseStack, popup: Popup, age: Float, time: Float, color: Int, fill: Int, outline: Int, chunky: Boolean, alpha: Float) {
        val count = popup.count
        val number = text(count.toString(), chunky, fill)
        val hits = text("HITS", chunky, fill)
        val width = maxOf(font.width(number) * SIZE, font.width(hits) * 1.25f, TIMER_WIDTH)
        poseStack.translate(-width / 2 + ComboCounter.shake(age, count, 0), -24f + ComboCounter.shake(age, count, 1), 0f)
        // A slight forward lean, like the counters in action games.
        poseStack.mulPose(Axis.ZP.rotation(-0.06f))
        val cx = width / 2
        val cy = 5f * SIZE

        if (ComboCounter.streaks(count)) streaks(queue, poseStack, time, width, color, alpha)
        if (ComboCounter.milestone(count)) burst(queue, poseStack, age, cx, cy, color, alpha)

        // The echo of the hit, growing out from behind the count.
        val (echoSize, echoAlpha) = ComboCounter.echo(age)
        if (echoAlpha > 0f) {
            poseStack.pushPose()
            poseStack.translate(cx, cy, -0.5f)
            poseStack.scale(SIZE * echoSize, SIZE * echoSize, SIZE * echoSize)
            draw(queue, poseStack, font, number, -font.width(number) / 2f, -4f, echoAlpha * alpha, 0)
            poseStack.popPose()
        }

        val size = SIZE * ComboCounter.pop(age, count) * ComboCounter.heartbeat(time, count)
        val flashed = text(count.toString(), chunky, DamageNumbers.mix(fill, DamageNumbers.WHITE, ComboCounter.flash(age, count)))
        poseStack.pushPose()
        poseStack.translate(cx, cy, 0f)
        poseStack.scale(size, size, size)
        draw(queue, poseStack, font, flashed, -font.width(flashed) / 2f, -4f, alpha, outline)
        poseStack.popPose()

        if (ComboCounter.rainbow(count)) sparkles(queue, poseStack, time, cx, cy, cx * 1.25f + 8f, popup, color, alpha)

        poseStack.pushPose()
        poseStack.translate(cx, 27f, 0f)
        poseStack.scale(1.25f, 1.25f, 1.25f)
        draw(queue, poseStack, font, hits, -font.width(hits) / 2f, 0f, alpha, outline)
        poseStack.popPose()

        poseStack.pushPose()
        poseStack.translate(cx - TIMER_WIDTH / 2, 0f, 0f)
        timer(queue, poseStack, ComboCounter.remaining(age), color, alpha)
        poseStack.popPose()
    }

    /** The combo's damage so far, slamming in a little like a damage number. */
    private fun total(font: Font, queue: SubmitNodeCollector, poseStack: PoseStack, age: Float, popup: Popup, fill: Int, outline: Int, chunky: Boolean, alpha: Float) {
        val text = text(DamageNumbers.format(popup.total) + " DMG", chunky, fill)
        val size = 1.3f * ComboCounter.pop(age, 0)
        poseStack.scale(size, size, size)
        draw(queue, poseStack, font, text, -font.width(text) / 2f, -4f, alpha, outline)
    }

    /** A new rank slamming in over the mob's head, in the number's colors, rainbow letter by letter at MAX. */
    private fun rank(font: Font, queue: SubmitNodeCollector, poseStack: PoseStack, popup: Popup, age: Float, time: Float, fill: Int, outline: Int, chunky: Boolean, alpha: Float) {
        val name = ComboCounter.rank(popup.count) ?: return
        val text = Component.empty()
        name.forEachIndexed { i, c ->
            text.append(text(c.toString(), chunky, if (popup.rainbow) DamageVfx.rainbow(i, time * 0.8f) else fill))
        }
        val scale = 1.6f * ComboCounter.rankScale(age)
        poseStack.mulPose(Axis.ZP.rotation(-0.05f + 0.03f * sin(time * 0.3f)))
        poseStack.scale(scale, scale, scale)
        draw(queue, poseStack, font, text, -font.width(text) / 2f, -4f, alpha, outline)
    }

    private const val TIMER_WIDTH = 56f

    /** The time left before the combo breaks, draining toward the left, with a white-hot tip. */
    private fun timer(queue: SubmitNodeCollector, poseStack: PoseStack, remaining: Float, color: Int, alpha: Float) {
        val left = TIMER_WIDTH * remaining
        val dim = DamageNumbers.withAlpha(color, 0.18f * alpha)
        val bright = DamageNumbers.withAlpha(color, alpha)
        val clear = DamageNumbers.withAlpha(color, 0f)
        val white = DamageNumbers.withAlpha(DamageNumbers.WHITE, alpha)
        DamageNumberPopups.glow(queue, poseStack) { pose, v ->
            DamageNumberPopups.quad(v, pose, -1f, 40f, dim, TIMER_WIDTH + 1, 40f, dim, TIMER_WIDTH + 1, 43f, dim, -1f, 43f, dim)
            if (left <= 0f) return@glow
            DamageNumberPopups.quad(v, pose, 0f, 41f, bright, left, 41f, bright, left, 42f, bright, 0f, 42f, bright)
            // A soft halo above and below the bar.
            DamageNumberPopups.quad(v, pose, 0f, 38.5f, clear, left, 38.5f, clear, left, 41f, bright, 0f, 41f, bright)
            DamageNumberPopups.quad(v, pose, 0f, 42f, bright, left, 42f, bright, left, 44.5f, clear, 0f, 44.5f, clear)
            DamageNumberPopups.diamond(v, pose, left - 3f, 41.5f, left + 3f, 41.5f, 2.5f, white)
        }
    }

    /** Speed streaks rushing right to left behind the count. */
    private fun streaks(queue: SubmitNodeCollector, poseStack: PoseStack, time: Float, width: Float, color: Int, alpha: Float) {
        DamageNumberPopups.glow(queue, poseStack) { pose, v ->
            for (i in 0 until 12) {
                val p = ComboCounter.streak(i, time)
                val y = -4f + (i * 37) % 54
                val length = 18f + (i * 11) % 26
                val x = -60f + (width + 120f) * p
                val c = if (i % 3 == 0) DamageNumbers.WHITE else color
                DamageNumberPopups.diamond(v, pose, x, y, x + length, y, 0.8f, DamageNumbers.withAlpha(c, 0.6f * alpha * sin(p * PI.toFloat())))
            }
        }
    }

    /** Every 10th hit: a ring of light flying out and shards bursting from the count. */
    private fun burst(queue: SubmitNodeCollector, poseStack: PoseStack, age: Float, cx: Float, cy: Float, color: Int, alpha: Float) {
        poseStack.pushPose()
        poseStack.translate(cx, cy, -1f)
        DamageNumberPopups.glow(queue, poseStack) { pose, v ->
            ComboCounter.shockwave(age)?.let { (radius, a) ->
                DamageNumberPopups.ring(v, pose, radius, 2f + 4f * a, color, a * alpha)
                DamageNumberPopups.ring(v, pose, radius * 0.8f, 1f + 2f * a, DamageNumbers.WHITE, a * alpha)
            }
            for (i in 0 until ComboCounter.SHARDS) {
                val (x, y, size) = ComboCounter.shard(i, age) ?: continue
                val spin = age * 0.5f + i
                val r = size * 1.6f
                val c = DamageNumbers.withAlpha(if (i % 3 == 0) DamageNumbers.WHITE else color, alpha)
                DamageNumberPopups.tri(
                    v, pose,
                    x + cos(spin) * r, y + sin(spin) * r, c,
                    x + cos(spin + 2.2f) * r, y + sin(spin + 2.2f) * r, c,
                    x + cos(spin + 4.1f) * r * 0.6f, y + sin(spin + 4.1f) * r * 0.6f, c,
                )
            }
        }
        poseStack.popPose()
    }

    /** Four-pointed sparkles twinkling around the count from 100 hits. */
    private fun sparkles(queue: SubmitNodeCollector, poseStack: PoseStack, time: Float, cx: Float, cy: Float, spread: Float, popup: Popup, color: Int, alpha: Float) {
        DamageNumberPopups.glow(queue, poseStack) { pose, v ->
            for (i in 0 until 8) {
                val glow = ComboCounter.twinkle(i, time)
                if (glow <= 0.05f) continue
                val angle = i * 0.785f + 0.3f
                val x = cx + cos(angle) * spread * (0.8f + (i % 3) * 0.15f)
                val y = cy + sin(angle) * 18f
                val c = DamageNumbers.withAlpha(if (popup.rainbow) DamageVfx.rainbow(i, time) else DamageNumbers.mix(color, DamageNumbers.WHITE, 0.5f * (i % 2)), glow * alpha)
                val s = 1f + 4f * glow
                DamageNumberPopups.diamond(v, pose, x - s, y, x + s, y, 0.3f * s, c)
                DamageNumberPopups.diamond(v, pose, x, y - s, x, y + s, 0.3f * s, c)
            }
        }
    }

    private fun text(value: String, chunky: Boolean, color: Int): MutableComponent {
        var s = Style.EMPTY.withBold(!chunky).withColor(TextColor.fromRgb(color and 0xFFFFFF))
        if (chunky) s = s.withFont(FONT)
        return Component.literal(value).withStyle(s)
    }

    /** Text glowing at full brightness, with [outline] around it, or none for 0. */
    private fun draw(queue: SubmitNodeCollector, poseStack: PoseStack, font: Font, text: Component, x: Float, y: Float, alpha: Float, outline: Int) {
        queue.submitText(poseStack, x, y, text.visualOrderText, false, Font.DisplayMode.NORMAL, LightTexture.FULL_BRIGHT, DamageNumbers.withAlpha(DamageNumbers.WHITE, alpha), 0, outline)
    }
}
