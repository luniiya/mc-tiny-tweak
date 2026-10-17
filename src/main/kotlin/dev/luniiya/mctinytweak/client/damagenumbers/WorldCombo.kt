package dev.luniiya.mctinytweak.client.damagenumbers

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.damagenumbers.ComboCounter
import dev.luniiya.mctinytweak.damagenumbers.DamageNumbers
import dev.luniiya.mctinytweak.damagenumbers.DamageVfx
import dev.luniiya.mctinytweak.healthbar.HealthBars
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.state.CameraRenderState
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The player's combo: their hits in a row, shown as a "24 HITS" counter floating beside the mob they hit
 * last, facing the camera. [DamageNumberPopups] draws it with the damage numbers.
 */
object WorldCombo {
    val combo = ComboCounter()

    private val FONT = FontDescription.Resource(Identifier.fromNamespaceAndPath(McTinyTweak.MOD_ID, "damage"))

    /** Pixels the count is drawn at before its pop, from the font's own size. */
    private const val SIZE = 2.6f

    /** The whole counter's size against a name tag's pixels. */
    private const val SCALE = 0.8f

    /** Blocks between the mob's side and the counter. */
    private const val GAP = 0.5

    /** The counter's layout is anchored on its right edge and this many pixels wide. */
    private const val LAYOUT_WIDTH = 62f

    /** Pixels from the layout's top to its middle, so it sits centered on the anchor height. */
    private const val LAYOUT_MIDDLE = 35f

    /** The mob hit last, and where it was, so the counter stays put if it dies. */
    var targetId = -1
        private set
    private var lastPos: Vec3 = Vec3.ZERO
    private var targetWidth = 0.6
    private var targetHeight = 1.8
    private var broken = true

    /** Counts one of the player's hits on [target], with sounds as the combo climbs. */
    fun hit(time: Long, damage: Float, target: LivingEntity) {
        val wasActive = combo.active(time)
        val rankBefore = ComboCounter.rankLevel(combo.count)
        combo.hit(time, damage)
        if (!wasActive) broken = false
        targetId = target.id
        lastPos = target.position()
        targetWidth = target.bbWidth.toDouble()
        targetHeight = target.bbHeight.toDouble()
        val style = DamageNumberPopups.style
        if (!style.comboCounter || !style.sounds) return
        val count = combo.count
        val rank = ComboCounter.rankLevel(count)
        if (ComboCounter.shown(count)) {
            // Each hit ticks a little higher through every 10, like a rising drum roll.
            DamageSounds.play(DamageSounds.COMBO_TICK, 0.35f, 0.8f + 0.06f * (count % ComboCounter.STEP) + 0.05f * minOf(rank, 5))
        }
        if (ComboCounter.milestone(count)) DamageSounds.play(DamageSounds.COMBO_MILESTONE, 0.7f, 0.9f + 0.08f * minOf(rank, 5))
        if (rank > rankBefore) DamageSounds.play(DamageSounds.COMBO_RANK, 0.8f, 0.9f + 0.06f * rank)
    }

    /** Plays the combo breaking, once, when a combo of 10 or more runs out. */
    fun tick(time: Long) {
        if (broken || combo.active(time)) return
        broken = true
        val style = DamageNumberPopups.style
        if (style.comboCounter && style.sounds && combo.count >= ComboCounter.STEP) DamageSounds.play(DamageSounds.COMBO_BREAK, 0.5f, 1f)
    }

    fun clear() {
        combo.clear()
        targetId = -1
        broken = true
    }

    /** True while the counter has something to draw. */
    fun visible(time: Long): Boolean {
        if (!DamageNumberPopups.style.comboCounter || !ComboCounter.shown(combo.count)) return false
        val since = combo.sinceHit(time) ?: return false
        return ComboCounter.alpha(since) > 0f
    }

    /** Where the counter floats from: the mob this frame, or where it was last seen. */
    private fun anchor(partial: Float): Vec3 {
        val entity = Minecraft.getInstance().level?.getEntity(targetId)
        if (entity is LivingEntity && entity.isAlive) lastPos = entity.getPosition(partial)
        return lastPos
    }

    fun render(font: Font, queue: SubmitNodeCollector, poseStack: PoseStack, camera: CameraRenderState, gameTime: Long, partial: Float) {
        if (!visible(gameTime)) return
        val since = combo.sinceHit(gameTime, partial) ?: return
        val alpha = ComboCounter.alpha(since)
        if (alpha <= 0f) return
        val time = gameTime + partial
        val count = combo.count
        val chunky = DamageNumberPopups.style.font
        val rainbow = ComboCounter.rainbow(count)
        val color = if (rainbow) DamageVfx.rainbow(0, time * 0.6f) else ComboCounter.color(count)

        val at = anchor(partial)
        DamageNumberPopups.billboard(poseStack, camera, at.x, at.y + targetHeight * 0.6, at.z, DamageNumbers.clearance(targetWidth))
        // Off to the camera's right of the mob, sliding further out as it fades once broken.
        val side = ((targetWidth / 2 + GAP) / 0.025).toFloat()
        poseStack.translate(side + LAYOUT_WIDTH * SCALE + ComboCounter.slide(since) * 40f, -LAYOUT_MIDDLE * SCALE, 0f)
        poseStack.scale(SCALE, SCALE, SCALE)
        poseStack.translate(ComboCounter.shake(since, count, 0), ComboCounter.shake(since, count, 1), 0f)
        // A slight forward lean, like the counters in action games.
        poseStack.mulPose(Axis.ZP.rotation(-0.06f))

        val number = text(count.toString(), chunky, color)
        val cx = -font.width(number) * SIZE / 2f
        val cy = 5f * SIZE

        if (ComboCounter.streaks(count)) streaks(queue, poseStack, time, color, alpha)
        combo.sinceMilestone(gameTime, partial)?.let { burst(queue, poseStack, it, cx, cy, color, alpha) }

        // The echo of the last hit, growing out from behind the count.
        val (echoSize, echoAlpha) = ComboCounter.echo(since)
        if (echoAlpha > 0f) {
            poseStack.pushPose()
            poseStack.translate(cx, cy, -0.5f)
            poseStack.scale(SIZE * echoSize, SIZE * echoSize, SIZE * echoSize)
            draw(queue, poseStack, font, number, -font.width(number) / 2f, -4f, echoAlpha * alpha, outline = false)
            poseStack.popPose()
        }

        val size = SIZE * ComboCounter.pop(since, count) * ComboCounter.heartbeat(time, count)
        val flash = ComboCounter.flash(since, count)
        poseStack.pushPose()
        poseStack.translate(cx, cy, 0f)
        poseStack.scale(size, size, size)
        val flashed = text(count.toString(), chunky, DamageNumbers.mix(color, DamageNumbers.WHITE, flash))
        draw(queue, poseStack, font, flashed, -font.width(flashed) / 2f, -4f, alpha)
        poseStack.popPose()

        if (rainbow) sparkles(queue, poseStack, time, cx, cy, -cx * 1.25f, alpha)

        val hits = text("HITS", chunky, DamageNumbers.WHITE)
        poseStack.pushPose()
        poseStack.translate(0f, 27f, 0f)
        poseStack.scale(1.25f, 1.25f, 1.25f)
        draw(queue, poseStack, font, hits, -font.width(hits).toFloat(), 0f, alpha)
        poseStack.popPose()

        timer(queue, poseStack, ComboCounter.remaining(since), color, alpha)

        val total = text(DamageNumbers.format(combo.total) + " DMG", chunky, DamageNumbers.mix(color, DamageNumbers.WHITE, 0.5f))
        draw(queue, poseStack, font, total, -font.width(total).toFloat(), 47f, alpha)

        ComboCounter.rank(count)?.let { rank(queue, poseStack, font, it, count, combo.sinceRank(gameTime, partial) ?: 99f, time, chunky, alpha) }
        poseStack.popPose()
    }

    /** The time left before the combo breaks, draining toward the right, with a white-hot tip. */
    private fun timer(queue: SubmitNodeCollector, poseStack: PoseStack, remaining: Float, color: Int, alpha: Float) {
        val width = 56f
        val left = width * remaining
        val dim = DamageNumbers.withAlpha(color, 0.18f * alpha)
        val bright = DamageNumbers.withAlpha(color, alpha)
        val clear = DamageNumbers.withAlpha(color, 0f)
        val white = DamageNumbers.withAlpha(DamageNumbers.WHITE, alpha)
        DamageNumberPopups.glow(queue, poseStack) { pose, v ->
            DamageNumberPopups.quad(v, pose, -width - 1, 40f, dim, 1f, 40f, dim, 1f, 43f, dim, -width - 1, 43f, dim)
            if (left <= 0f) return@glow
            DamageNumberPopups.quad(v, pose, -left, 41f, bright, 0f, 41f, bright, 0f, 42f, bright, -left, 42f, bright)
            // A soft halo above and below the bar.
            DamageNumberPopups.quad(v, pose, -left, 38.5f, clear, 0f, 38.5f, clear, 0f, 41f, bright, -left, 41f, bright)
            DamageNumberPopups.quad(v, pose, -left, 42f, bright, 0f, 42f, bright, 0f, 44.5f, clear, -left, 44.5f, clear)
            DamageNumberPopups.diamond(v, pose, -left - 3f, 41.5f, -left + 3f, 41.5f, 2.5f, white)
        }
    }

    /** The rank under the counter, slamming in when reached, colored by how high it is. */
    private fun rank(queue: SubmitNodeCollector, poseStack: PoseStack, font: Font, rank: String, count: Int, sinceRank: Float, time: Float, chunky: Boolean, alpha: Float) {
        val level = ComboCounter.rankLevel(count)
        val text = Component.empty()
        rank.forEachIndexed { i, c ->
            val color = if (ComboCounter.rainbow(count)) DamageVfx.rainbow(i, time * 0.8f) else DamageNumbers.mix(HealthBars.layerColor(level), HealthBars.GOLD, 0.5f + 0.5f * sin(time * 0.4f - i * 0.6f))
            text.append(text(c.toString(), chunky, color))
        }
        val scale = 1.4f * ComboCounter.rankScale(sinceRank)
        poseStack.pushPose()
        poseStack.translate(-font.width(text) * 0.7f, 64f, 0f)
        poseStack.mulPose(Axis.ZP.rotation(-0.05f + 0.03f * sin(time * 0.3f)))
        poseStack.scale(scale, scale, scale)
        draw(queue, poseStack, font, text, -font.width(text) / 2f, -4f, alpha)
        poseStack.popPose()
    }

    /** Speed streaks rushing right to left behind the counter. */
    private fun streaks(queue: SubmitNodeCollector, poseStack: PoseStack, time: Float, color: Int, alpha: Float) {
        DamageNumberPopups.glow(queue, poseStack) { pose, v ->
            for (i in 0 until 12) {
                val p = ComboCounter.streak(i, time)
                val y = -4f + (i * 37) % 64
                val length = 18f + (i * 11) % 26
                val x = -150f + 175f * p
                val c = if (i % 3 == 0) DamageNumbers.WHITE else color
                DamageNumberPopups.diamond(v, pose, x, y, x + length, y, 0.8f, DamageNumbers.withAlpha(c, 0.6f * alpha * sin(p * PI.toFloat())))
            }
        }
    }

    /** Every 10th hit: a ring of light flying out and shards bursting from the count. */
    private fun burst(queue: SubmitNodeCollector, poseStack: PoseStack, sinceMilestone: Float, cx: Float, cy: Float, color: Int, alpha: Float) {
        poseStack.pushPose()
        poseStack.translate(cx, cy, -1f)
        DamageNumberPopups.glow(queue, poseStack) { pose, v ->
            ComboCounter.shockwave(sinceMilestone)?.let { (radius, a) ->
                DamageNumberPopups.ring(v, pose, radius, 2f + 4f * a, color, a * alpha)
                DamageNumberPopups.ring(v, pose, radius * 0.8f, 1f + 2f * a, DamageNumbers.WHITE, a * alpha)
            }
            for (i in 0 until ComboCounter.SHARDS) {
                val (x, y, size) = ComboCounter.shard(i, sinceMilestone) ?: continue
                val spin = sinceMilestone * 0.5f + i
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

    /** Four-pointed sparkles twinkling around the count once it is rainbow. */
    private fun sparkles(queue: SubmitNodeCollector, poseStack: PoseStack, time: Float, cx: Float, cy: Float, spread: Float, alpha: Float) {
        DamageNumberPopups.glow(queue, poseStack) { pose, v ->
            for (i in 0 until 8) {
                val glow = ComboCounter.twinkle(i, time)
                if (glow <= 0.05f) continue
                val angle = i * 0.785f + 0.3f
                val x = cx + cos(angle) * spread * (0.8f + (i % 3) * 0.15f)
                val y = cy + sin(angle) * 18f
                val c = DamageNumbers.withAlpha(DamageVfx.rainbow(i, time), glow * alpha)
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

    /** Text glowing at full brightness, with a dark outline so it reads over anything. */
    private fun draw(queue: SubmitNodeCollector, poseStack: PoseStack, font: Font, text: Component, x: Float, y: Float, alpha: Float, outline: Boolean = true) {
        val outlineColor = if (outline) DamageNumbers.withAlpha(DamageNumbers.BLACK, 0.85f * alpha) else 0
        queue.submitText(poseStack, x, y, text.visualOrderText, false, Font.DisplayMode.NORMAL, LightTexture.FULL_BRIGHT, DamageNumbers.withAlpha(DamageNumbers.WHITE, alpha), 0, outlineColor)
    }
}
