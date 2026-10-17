package dev.luniiya.mctinytweak.damagenumbers

import dev.luniiya.mctinytweak.healthbar.HealthBars
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Counts the player's hits in a row, like the "24 HITS" counter in gacha games. The combo breaks when no hit
 * lands for [BREAK_TICKS], then the counter fades out over [FADE_TICKS]. The longer the combo, the more
 * goes on around it:
 * - every hit: the count slams in, shakes and leaves an expanding echo of itself.
 * - from 10: each hit flashes white, and every 10th bursts shards and a shockwave.
 * - ranks: NICE, GREAT, AWESOME, INSANE and GODLIKE pop in as the combo climbs.
 * - from 50: speed streaks rush past behind it.
 * - from 100: rainbow, with sparkles twinkling around it.
 */
class ComboCounter {
    var count = 0
        private set

    /** Damage dealt over the whole combo. */
    var total = 0f
        private set

    private var last = Long.MIN_VALUE

    /** When the count last reached a [STEP], and when it last reached a new rank. */
    var milestoneAt = Long.MIN_VALUE
        private set
    var rankAt = Long.MIN_VALUE
        private set

    fun hit(time: Long, damage: Float) {
        if (!active(time)) {
            count = 0
            total = 0f
            milestoneAt = Long.MIN_VALUE
            rankAt = Long.MIN_VALUE
        }
        val rankBefore = rank(count)
        count++
        total += damage
        last = time
        if (milestone(count)) milestoneAt = time
        if (rank(count) != rankBefore) rankAt = time
    }

    /** True while another hit still adds to the combo. */
    fun active(time: Long) = last != Long.MIN_VALUE && time - last < BREAK_TICKS

    /** Ticks since the last hit, or null before any. [partial] is the fraction of a tick since [time]. */
    fun sinceHit(time: Long, partial: Float = 0f): Float? = since(last, time, partial)

    fun sinceMilestone(time: Long, partial: Float = 0f): Float? = since(milestoneAt, time, partial)

    fun sinceRank(time: Long, partial: Float = 0f): Float? = since(rankAt, time, partial)

    private fun since(at: Long, time: Long, partial: Float) = if (at == Long.MIN_VALUE) null else time - at + partial

    fun clear() {
        count = 0
        total = 0f
        last = Long.MIN_VALUE
        milestoneAt = Long.MIN_VALUE
        rankAt = Long.MIN_VALUE
    }

    companion object {
        /** Ticks without a hit that break the combo. A full sword swing takes about 13. */
        const val BREAK_TICKS = 50

        /** Ticks the counter takes to fade out once broken. */
        const val FADE_TICKS = 10

        /** Ticks each hit's pop takes to settle. */
        const val POP_TICKS = 5

        /** Ticks each hit's echo takes to spread and fade. */
        const val ECHO_TICKS = 7

        /** Ticks a milestone's shards and shockwave fly. */
        const val BURST_TICKS = 12

        /** Ticks a new rank takes to pop in. */
        const val RANK_TICKS = 6

        /** The counter shows from this many hits. */
        const val MIN_SHOWN = 2

        /** Every this many hits, the counter climbs a heart color and hits harder. */
        const val STEP = 10

        /** From this many hits each one flashes white. */
        const val FLASH_FROM = 10

        /** From this many hits speed streaks rush behind the counter. */
        const val STREAKS_FROM = 50

        /** From this many hits the counter turns rainbow and sparkles. */
        const val RAINBOW = 100

        /** How close to the mob's middle the counter's pieces start on each hit, against their spot. */
        const val SPREAD_FROM = 0.55f

        /** Ticks between each piece of the counter flying out. */
        const val SPREAD_STAGGER = 0.6f

        /** Shards a milestone bursts. */
        const val SHARDS = 14

        /** The ranks and the hits they take. */
        private val RANKS = listOf(10 to "NICE", 20 to "GREAT", 30 to "AWESOME", 50 to "INSANE", 100 to "GODLIKE")

        fun shown(count: Int) = count >= MIN_SHOWN

        /** The rank a combo of [count] has reached, or null below the first. */
        fun rank(count: Int): String? = RANKS.lastOrNull { count >= it.first }?.second

        /** How high the rank is, from 0 for none to 5 for GODLIKE. */
        fun rankLevel(count: Int): Int = RANKS.count { count >= it.first }

        /** How much of the time to the next hit is left, from 1 right after a hit to 0 when the combo breaks. */
        fun remaining(sinceHit: Float): Float = clamp(1f - sinceHit / BREAK_TICKS)

        /** The counter's opacity: solid while the combo lives, then fading out. */
        fun alpha(sinceHit: Float): Float = clamp(1f - (sinceHit - BREAK_TICKS) / FADE_TICKS)

        /** How far the counter has slid out to the right after breaking, from 0 to 1. */
        fun slide(sinceHit: Float): Float = clamp((sinceHit - BREAK_TICKS) / FADE_TICKS).let { it * it }

        /** True on the hits that reach a new [STEP], which pop bigger. */
        fun milestone(count: Int) = count >= STEP && count % STEP == 0

        /** The count's size: slamming in on each hit and settling, harder for longer combos and milestones. */
        fun pop(sinceHit: Float, count: Int): Float {
            val peak = if (milestone(count)) 2.3f else 1.4f + 0.06f * minOf(rankLevel(count), 5)
            val t = clamp(sinceHit / POP_TICKS)
            return peak + (1f - peak) * easeOutBack(t)
        }

        /** A slow heartbeat on long combos, so the counter never sits still. */
        fun heartbeat(time: Float, count: Int): Float = if (rankLevel(count) < 3) 1f else 1f + 0.04f * rankLevel(count) * maxOf(0f, sin(time * 0.5f)).let { it * it * it }

        /** Shake in pixels right after a hit, harder for longer combos and milestones. */
        fun shake(sinceHit: Float, count: Int, axis: Int): Float {
            val left = 1f - clamp(sinceHit / POP_TICKS)
            val amount = (if (milestone(count)) 3.5f else 1f + 0.3f * rankLevel(count)) * left
            return amount * sin(sinceHit * (5.3f + axis * 1.7f) + axis)
        }

        /** How white the count flashes right after a hit, from [FLASH_FROM] hits. */
        fun flash(sinceHit: Float, count: Int): Float = if (count < FLASH_FROM) 0f else 1f - clamp(sinceHit / 2.5f)

        /** Each hit's echo: a copy of the count growing out and fading. Returns its size and opacity. */
        fun echo(sinceHit: Float): Pair<Float, Float> {
            val t = clamp(sinceHit / ECHO_TICKS)
            return (1f + 1.4f * easeOutCubic(t)) to 0.7f * (1f - t)
        }

        /** The milestone shockwave's radius in pixels and its opacity, or null once it is over. */
        fun shockwave(sinceMilestone: Float): Pair<Float, Float>? {
            val t = sinceMilestone / (BURST_TICKS * 0.7f)
            if (t < 0f || t >= 1f) return null
            return 8f + 46f * easeOutCubic(t) to 1f - t * t
        }

        /** Where milestone shard [index] has flown, in pixels, how far it has spun, and its size; null when gone. */
        fun shard(index: Int, sinceMilestone: Float): Triple<Float, Float, Float>? {
            val t = sinceMilestone / BURST_TICKS
            if (t < 0f || t >= 1f) return null
            val angle = (index * 2 * PI / SHARDS).toFloat() + index * 0.37f
            val out = (26f + (index * 7) % 18) * easeOutCubic(t)
            val fall = 24f * t * t
            return Triple(cos(angle) * out, sin(angle) * out + fall, 3.5f * (1f - t))
        }

        /**
         * How far piece [order] of the counter has flown from the mob's middle to its spot around the mob after a
         * hit: from [SPREAD_FROM] right after it, overshooting a little, then settling on 1. Later pieces leave a
         * moment after the first, and once the combo breaks they all drift further out as they fade.
         */
        fun spread(sinceHit: Float, order: Int): Float {
            val t = clamp((sinceHit - order * SPREAD_STAGGER) / POP_TICKS)
            return (SPREAD_FROM + (1f - SPREAD_FROM) * easeOutBack(t)) * (1f + 0.5f * slide(sinceHit))
        }

        /** The new rank's size: slamming in from big with a bounce, then a little wobble. */
        fun rankScale(sinceRank: Float): Float {
            val t = clamp(sinceRank / RANK_TICKS)
            return (2.6f + (1f - 2.6f) * easeOutBack(t)) * (1f + 0.05f * sin(sinceRank * 0.9f))
        }

        /** Streak [index]'s position from 1 (just appearing on the right) to 0 (gone on the left), looping. */
        fun streak(index: Int, time: Float): Float {
            val speed = 0.09f + (index * 13 % 7) * 0.012f
            val phase = time * speed + index * 0.618f
            return 1f - (phase - kotlin.math.floor(phase))
        }

        /** How bright sparkle [index] twinkles at [time], from 0 to 1. */
        fun twinkle(index: Int, time: Float): Float = maxOf(0f, sin(time * (0.35f + index * 0.07f) + index * 2.1f)).let { it * it }

        /** Climbs the heart colors every [STEP] hits, like damage climbs them every doubling. */
        fun color(count: Int): Int = HealthBars.layerColor(count / STEP)

        fun rainbow(count: Int) = count >= RAINBOW

        fun streaks(count: Int) = count >= STREAKS_FROM

        private fun clamp(value: Float) = value.coerceIn(0f, 1f)

        private fun easeOutCubic(t: Float): Float {
            val u = 1f - t
            return 1f - u * u * u
        }

        private fun easeOutBack(t: Float): Float {
            val c1 = 1.70158f
            val c3 = c1 + 1f
            val u = t - 1f
            return 1f + c3 * u * u * u + c1 * u * u
        }
    }
}
