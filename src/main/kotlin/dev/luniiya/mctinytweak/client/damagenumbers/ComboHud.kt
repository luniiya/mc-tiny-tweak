package dev.luniiya.mctinytweak.client.damagenumbers

import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.client.tweaks.Tweaks
import dev.luniiya.mctinytweak.damagenumbers.ComboCounter
import dev.luniiya.mctinytweak.damagenumbers.DamageNumbers
import dev.luniiya.mctinytweak.damagenumbers.DamageVfx
import dev.luniiya.mctinytweak.healthbar.HealthBars
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The gacha-style "24 HITS" counter on the right of the screen, for the player's own hits in a row. */
object ComboHud {
    val combo = ComboCounter()

    private val FONT = FontDescription.Resource(Identifier.fromNamespaceAndPath(McTinyTweak.MOD_ID, "damage"))

    /** Pixels the count is drawn at before its pop, from the font's own size. */
    private const val SIZE = 2.6f

    fun register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(McTinyTweak.MOD_ID, "combo_counter"), ::render)
    }

    /** Counts one of the player's hits, with a chime on every 10th that climbs with the rank. */
    fun hit(time: Long, damage: Float) {
        val rankBefore = ComboCounter.rankLevel(combo.count)
        combo.hit(time, damage)
        val style = DamageNumberPopups.style
        if (!style.comboCounter || !style.sounds) return
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        val level = minecraft.level ?: return
        val rank = ComboCounter.rankLevel(combo.count)
        if (ComboCounter.milestone(combo.count)) {
            level.playLocalSound(player.x, player.eyeY, player.z, SoundEvents.NOTE_BLOCK_CHIME.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.8f + 0.15f * minOf(rank, 8), false)
        }
        if (rank > rankBefore) {
            level.playLocalSound(player.x, player.eyeY, player.z, SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1f + 0.1f * rank, false)
        }
    }

    private fun render(graphics: GuiGraphics, delta: DeltaTracker) {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return
        if (!Tweaks.damageNumbers() || !DamageNumberPopups.style.comboCounter || !ComboCounter.shown(combo.count)) return
        val partial = delta.getGameTimeDeltaPartialTick(false)
        val since = combo.sinceHit(level.gameTime, partial) ?: return
        val alpha = ComboCounter.alpha(since)
        if (alpha <= 0f) return
        val time = level.gameTime + partial
        val font = minecraft.font
        val count = combo.count
        val chunky = DamageNumberPopups.style.font
        val rainbow = ComboCounter.rainbow(count)
        val color = if (rainbow) DamageVfx.rainbow(0, time * 0.6f) else ComboCounter.color(count)

        val right = graphics.guiWidth() - 14f + ComboCounter.slide(since) * 90f
        val top = graphics.guiHeight() / 2f - 34f
        val pose = graphics.pose()

        pose.pushMatrix()
        pose.translate(right + ComboCounter.shake(since, count, 0), top + ComboCounter.shake(since, count, 1))
        // A slight forward lean, like the counters in action games.
        pose.rotate(-0.06f)

        val number = text(count.toString(), chunky)
        val cx = -font.width(number) * SIZE / 2f
        val cy = 5f * SIZE

        if (ComboCounter.streaks(count)) streaks(graphics, time, color, alpha)
        combo.sinceMilestone(level.gameTime, partial)?.let { burst(graphics, it, cx, cy, color, alpha) }

        // The echo of the last hit, growing out from behind the count.
        val (echoSize, echoAlpha) = ComboCounter.echo(since)
        if (echoAlpha > 0f) {
            pose.pushMatrix()
            pose.translate(cx, cy)
            pose.scale(SIZE * echoSize, SIZE * echoSize)
            graphics.drawString(font, number, -font.width(number) / 2, -4, DamageNumbers.withAlpha(color, echoAlpha * alpha), false)
            pose.popMatrix()
        }

        val size = SIZE * ComboCounter.pop(since, count) * ComboCounter.heartbeat(time, count)
        val flash = ComboCounter.flash(since, count)
        pose.pushMatrix()
        pose.translate(cx, cy)
        pose.scale(size, size)
        outlined(graphics, font, number, -font.width(number) / 2, -4, DamageNumbers.withAlpha(DamageNumbers.mix(color, DamageNumbers.WHITE, flash), alpha), alpha)
        pose.popMatrix()

        if (rainbow) sparkles(graphics, time, cx, cy, -cx * 1.25f, alpha)

        val hits = text("HITS", chunky)
        pose.pushMatrix()
        pose.translate(0f, 27f)
        pose.scale(1.25f, 1.25f)
        outlined(graphics, font, hits, -font.width(hits), 0, DamageNumbers.withAlpha(DamageNumbers.WHITE, alpha), alpha)
        pose.popMatrix()

        // The time left before the combo breaks, draining toward the right, glowing white at its tip.
        val barWidth = 56
        val left = (barWidth * ComboCounter.remaining(since)).toInt()
        graphics.fill(-barWidth - 1, 40, 1, 43, DamageNumbers.withAlpha(DamageNumbers.BLACK, 0.6f * alpha))
        graphics.fill(-left, 41, 0, 42, DamageNumbers.withAlpha(color, alpha))
        if (left > 0) graphics.fill(-left, 40, -left + 2, 43, DamageNumbers.withAlpha(DamageNumbers.WHITE, alpha))

        val total = text(DamageNumbers.format(combo.total) + " DMG", chunky)
        outlined(graphics, font, total, -font.width(total), 47, DamageNumbers.withAlpha(DamageNumbers.mix(color, DamageNumbers.WHITE, 0.5f), alpha), alpha)

        ComboCounter.rank(count)?.let { rank(graphics, font, it, count, combo.sinceRank(level.gameTime, partial) ?: 99f, time, chunky, alpha) }
        pose.popMatrix()
    }

    /** The rank under the counter, slamming in when reached, colored by how high it is. */
    private fun rank(graphics: GuiGraphics, font: Font, rank: String, count: Int, sinceRank: Float, time: Float, chunky: Boolean, alpha: Float) {
        val level = ComboCounter.rankLevel(count)
        val text = Component.empty()
        rank.forEachIndexed { i, c ->
            val color = if (ComboCounter.rainbow(count)) DamageVfx.rainbow(i, time * 0.8f) else DamageNumbers.mix(HealthBars.layerColor(level), HealthBars.GOLD, 0.5f + 0.5f * sin(time * 0.4f - i * 0.6f))
            text.append(Component.literal(c.toString()).withStyle(textStyle(chunky).withColor(color and 0xFFFFFF)))
        }
        val scale = 1.4f * ComboCounter.rankScale(sinceRank)
        val pose = graphics.pose()
        pose.pushMatrix()
        pose.translate(-font.width(text) * 0.7f, 64f)
        pose.rotate(-0.05f + 0.03f * sin(time * 0.3f))
        pose.scale(scale, scale)
        outlined(graphics, font, text, -font.width(text) / 2, -4, DamageNumbers.withAlpha(DamageNumbers.WHITE, alpha), alpha, outlineText = text(rank, chunky))
        pose.popMatrix()
    }

    /** Speed streaks rushing right to left behind the counter. */
    private fun streaks(graphics: GuiGraphics, time: Float, color: Int, alpha: Float) {
        for (i in 0 until 12) {
            val p = ComboCounter.streak(i, time)
            val y = -4 + (i * 37) % 64
            val length = 18 + (i * 11) % 26
            val x = (-150 + 175 * p).toInt()
            val c = if (i % 3 == 0) DamageNumbers.WHITE else color
            graphics.fill(x, y, x + length, y + 1, DamageNumbers.withAlpha(c, 0.55f * alpha * sin(p * PI.toFloat())))
        }
    }

    /** Every 10th hit: a ring of light flying out and shards bursting from the count. */
    private fun burst(graphics: GuiGraphics, sinceMilestone: Float, cx: Float, cy: Float, color: Int, alpha: Float) {
        val pose = graphics.pose()
        ComboCounter.shockwave(sinceMilestone)?.let { (radius, a) ->
            val segments = 28
            for (s in 0 until segments) {
                val angle = (s * 2 * PI / segments).toFloat()
                pose.pushMatrix()
                pose.translate(cx + cos(angle) * radius, cy + sin(angle) * radius)
                pose.rotate(angle + PI.toFloat() / 2)
                pose.scale(radius * 0.11f, 1f + 2f * a)
                graphics.fill(-1, -1, 1, 1, DamageNumbers.withAlpha(if (s % 2 == 0) DamageNumbers.WHITE else color, a * alpha))
                pose.popMatrix()
            }
        }
        for (i in 0 until ComboCounter.SHARDS) {
            val (x, y, size) = ComboCounter.shard(i, sinceMilestone) ?: continue
            pose.pushMatrix()
            pose.translate(cx + x, cy + y)
            pose.rotate(sinceMilestone * 0.5f + i)
            pose.scale(size, size)
            graphics.fill(-1, -1, 1, 1, DamageNumbers.withAlpha(if (i % 3 == 0) DamageNumbers.WHITE else color, alpha))
            pose.popMatrix()
        }
    }

    /** Four-pointed sparkles twinkling around the count once it is rainbow. */
    private fun sparkles(graphics: GuiGraphics, time: Float, cx: Float, cy: Float, spread: Float, alpha: Float) {
        val pose = graphics.pose()
        for (i in 0 until 8) {
            val glow = ComboCounter.twinkle(i, time)
            if (glow <= 0.05f) continue
            val angle = i * 0.785f + 0.3f
            pose.pushMatrix()
            pose.translate(cx + cos(angle) * spread * (0.8f + (i % 3) * 0.15f), cy + sin(angle) * 18f)
            pose.rotate(time * 0.1f)
            val c = DamageNumbers.withAlpha(DamageVfx.rainbow(i, time), glow * alpha)
            val s = 1f + 3f * glow
            pose.scale(s, s)
            graphics.fill(-2, 0, 2, 1, c)
            graphics.fill(0, -2, 1, 2, c)
            pose.popMatrix()
        }
    }

    private fun textStyle(chunky: Boolean): Style {
        var s = Style.EMPTY.withBold(!chunky)
        if (chunky) s = s.withFont(FONT)
        return s
    }

    private fun text(value: String, chunky: Boolean): Component = Component.literal(value).withStyle(textStyle(chunky))

    /** Text with a 1 pixel dark outline, so it reads over anything. */
    private fun outlined(graphics: GuiGraphics, font: Font, text: Component, x: Int, y: Int, color: Int, alpha: Float, outlineText: Component = text) {
        val outline = DamageNumbers.withAlpha(DamageNumbers.BLACK, 0.85f * alpha)
        // Colored letters would color their own outline, so it is drawn from an uncolored copy.
        for ((dx, dy) in OFFSETS) graphics.drawString(font, outlineText, x + dx, y + dy, outline, false)
        graphics.drawString(font, text, x, y, color, false)
    }

    private val OFFSETS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
}
