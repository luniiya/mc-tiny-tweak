package dev.luniiya.mctinytweak.damagenumbers

import dev.luniiya.mctinytweak.healthbar.HealthBars

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Floating damage numbers, worked out on the client from the health the server already syncs for every
 * living entity: when an entity's health drops between two ticks, the drop becomes a number.
 *
 * Ages are in ticks and may be fractional, so animations stay smooth between ticks.
 */
object DamageNumbers {
    /** Ticks a number stays on screen. */
    const val LIFETIME = 24

    /** Ticks the small pop-in takes when slam is off. */
    const val POP_TICKS = 4

    /** Ticks the slam takes to bounce back to size. */
    const val SLAM_TICKS = 7

    /** Ticks of fading out at the end. */
    const val FADE_TICKS = 8

    /** Ticks a big hit stays white-hot. */
    const val FLASH_TICKS = 4

    /** Ticks a big hit shakes. */
    const val SHAKE_TICKS = 7

    /** Ticks a number takes to count up to its value. */
    const val COUNT_TICKS = 5

    /** A new hit within this many ticks of the last one on the same entity adds to its combo. */
    const val COMBO_TICKS = 10

    /** How long after a crit animation or a kill damage event a health drop still belongs to it. */
    const val MARK_WINDOW = 2

    /** From this intensity a hit counts as big and gets flash, shake and sparks. */
    const val BIG = 0.5f

    /** From this intensity a hit counts as huge and gets the gradient. */
    const val HUGE = 0.8f

    /** Blocks a number is pulled toward the camera, enough to clear burning flames around the mob. */
    const val PULL = 1.5

    /** Drops smaller than this are rounding noise. */
    private const val MIN_DAMAGE = 0.05f

    const val WHITE = 0xFFFFFFFF.toInt()

    const val BLACK = 0xFF000000.toInt()

    /** Hits under this use the first heart color; each color after that is for twice as much damage. */
    const val FIRST_TIER = 5f

    /** What `/kill` shows instead of a number. */
    const val NULL_TEXT = "NULL"

    /** Characters a glitching NULL flickers into. */
    private const val GLITCH_CHARS = "#%&@!?/\\<>01_"

    const val GLITCH_RED = 0xFFFF2050.toInt()
    const val GLITCH_CYAN = 0xFF20F0FF.toInt()

    /** Remembers each entity's last health and reports drops. */
    class HealthTracker {
        private val last = HashMap<Int, Float>()

        /** Returns how much health [id] lost since the last call, or null for a new entity or no loss. */
        fun update(id: Int, health: Float): Float? {
            val before = last.put(id, health) ?: return null
            val lost = before - health
            return if (lost >= MIN_DAMAGE) lost else null
        }

        /** Forgets entities that are gone, so a reused id does not compare against a stale health. */
        fun retain(ids: Set<Int>) {
            last.keys.retainAll(ids)
        }

        fun clear() = last.clear()

        val size get() = last.size
    }

    /**
     * Remembers when the server flagged a hit on an entity, like a crit animation or a `/kill` damage event.
     * Both arrive just before the health update they belong to.
     */
    class Marks {
        private val at = HashMap<Int, Long>()

        fun mark(id: Int, time: Long) {
            at[id] = time
        }

        /** True if [id] was marked within [MARK_WINDOW] ticks before [time]. Each mark is only used once. */
        fun take(id: Int, time: Long): Boolean {
            val crit = at.remove(id) ?: return false
            return time - crit in 0..MARK_WINDOW
        }

        fun prune(time: Long) {
            at.values.removeIf { time - it > MARK_WINDOW }
        }

        fun clear() = at.clear()
    }

    /** Ticks a mob stays invulnerable after a hit; harder hits within it land without a new damage event. */
    const val HURT_TICKS = 20

    /**
     * Remembers who last hurt each entity, from the damage events the server sends. Hits that land while the
     * entity is still invulnerable from the last one come without an event, so they keep the last attacker
     * for [HURT_TICKS].
     */
    class Attackers {
        private val last = HashMap<Int, Pair<Int, Long>>()

        /** [cause] is the attacking entity's id, or -1 for none, like the packet sends it. */
        fun record(id: Int, cause: Int, time: Long) {
            last[id] = cause to time
        }

        /** Who hurt [id] within [HURT_TICKS] before [time], or null if no one we know of. */
        fun of(id: Int, time: Long): Int? {
            val (cause, at) = last[id] ?: return null
            return cause.takeIf { cause >= 0 && time - at in 0..HURT_TICKS }
        }

        fun prune(time: Long) {
            last.values.removeIf { time - it.second > HURT_TICKS }
        }

        fun clear() = last.clear()
    }

    /** Whether a hit should get a number: everyone's, or with [onlyMine] only those [attacker] is [me] for. */
    fun shows(onlyMine: Boolean, attacker: Int?, me: Int) = !onlyMine || attacker == me

    /**
     * The cosine of the widest angle from the view direction that is still on screen, from the vertical
     * [fovDegrees] and the screen's [aspect], with a little margin so things at the edge count.
     */
    fun viewCos(fovDegrees: Double, aspect: Double): Double {
        val half = Math.atan(Math.tan(Math.toRadians(fovDegrees) / 2) * maxOf(aspect, 1.0))
        return Math.cos(minOf(half + Math.toRadians(5.0), Math.toRadians(89.0)))
    }

    /** True if a spot ([dx], [dy], [dz]) from the camera is within [viewCos] of the camera's [forward] direction. */
    fun inView(dx: Double, dy: Double, dz: Double, fx: Double, fy: Double, fz: Double, viewCos: Double): Boolean {
        val length = Math.sqrt(dx * dx + dy * dy + dz * dz)
        if (length < 1e-6) return true
        return (dx * fx + dy * fy + dz * fz) / length >= viewCos
    }

    /** `6` for whole numbers and anything from 10 up, otherwise one decimal like `4.5`. */
    fun format(damage: Float): String {
        val tenths = (damage * 10).roundToInt()
        return if (damage >= 10 || tenths % 10 == 0) (tenths / 10.0).roundToInt().toString() else "${tenths / 10}.${tenths % 10}"
    }

    /** The text shown: the number, and a ✦ for crits when [critMark] is on. */
    fun label(damage: Float, crit: Boolean, critMark: Boolean) = format(damage) + if (crit && critMark) "✦" else ""

    /** Which heart color a hit gets: 0 under [FIRST_TIER], then one more for every doubling. */
    fun tier(damage: Float): Int = if (damage < FIRST_TIER) 0 else floor(log2(damage / FIRST_TIER)).toInt() + 1

    /** The health bar's layer colors by [tier], so bigger hits climb the same palette. */
    fun color(damage: Float): Int = HealthBars.layerColor(tier(damage))

    /** The digits' color. Crits flip the brightness: a dark fill instead of a bright one. */
    fun fill(color: Int, crit: Boolean): Int = if (crit) mix(BLACK, color, 0.3f) else color

    /** The outline's color: dark around normal hits, bright around crits. */
    fun outline(color: Int, crit: Boolean): Int = if (crit) mix(color, WHITE, 0.45f) else mix(BLACK, color, 0.25f)

    /** How hard a hit feels, from 0 for 4 damage or less to 1 for 32 or more. Crits count a bit harder. */
    fun intensity(damage: Float, crit: Boolean): Float {
        val byDamage = if (damage <= 4f) 0f else (ln(damage / 4f) / ln(8f)).toFloat()
        return clamp(byDamage + if (crit) 0.35f else 0f)
    }

    fun isBig(intensity: Float, crit: Boolean) = crit || intensity >= BIG

    fun isHuge(intensity: Float, crit: Boolean) = crit || intensity >= HUGE

    /**
     * Size multiplier. Harder hits stay bigger. With [slam] they start much bigger and bounce back past their
     * size before settling; without it they only pop in a little.
     */
    fun scale(age: Float, intensity: Float, slam: Boolean): Float {
        val base = 1f + 0.6f * intensity
        if (!slam) return base * (1f + 0.6f * (1f - clamp(age / POP_TICKS)))
        val peak = 1.6f + 1.8f * intensity
        val settle = easeOutBack(clamp(age / SLAM_TICKS))
        return base * (peak + (1f - peak) * settle)
    }

    /** How white a big hit still is, from 1 when it lands to 0 after [FLASH_TICKS]. */
    fun flash(age: Float): Float = 1f - clamp(age / FLASH_TICKS)

    /** Shake offset in text pixels, dying out over [SHAKE_TICKS]. [seed] keeps numbers from shaking in sync. */
    fun shakeX(age: Float, intensity: Float, seed: Float) = shakeAmount(age, intensity) * sin(age * 5.3f + seed)

    fun shakeY(age: Float, intensity: Float, seed: Float) = shakeAmount(age, intensity) * cos(age * 4.1f + seed * 1.7f)

    private fun shakeAmount(age: Float, intensity: Float) = (1.5f + 2.5f * intensity) * (1f - clamp(age / SHAKE_TICKS))

    /** The value shown while counting up: quick at first, landing on [damage] after [COUNT_TICKS]. */
    fun countUp(damage: Float, age: Float): Float = damage * easeOutCubic(clamp(age / COUNT_TICKS))

    /**
     * The color of the digit at [index] for gradient numbers: the hit's color, the next heart color and gold,
     * flowing across the digits over time.
     */
    fun gradientColor(damage: Float, index: Int, age: Float): Int {
        val next = HealthBars.layerColor(tier(damage) + 1)
        val stops = intArrayOf(color(damage), next, HealthBars.GOLD, next)
        val phase = index * 0.3f + age * 0.15f
        val i = floor(phase).toInt()
        return mix(stops[Math.floorMod(i, stops.size)], stops[Math.floorMod(i + 1, stops.size)], phase - i)
    }

    /** NULL with a letter or two flickering into glitch characters now and then. Same [tick] and [seed], same text. */
    fun glitchText(tick: Int, seed: Int): String {
        val chars = NULL_TEXT.toCharArray()
        val h = hash(tick, seed)
        if (h % 100 < 45) chars[(h shr 8) % chars.size] = GLITCH_CHARS[(h shr 12) % GLITCH_CHARS.length]
        if (h % 100 < 12) chars[(h shr 16) % chars.size] = GLITCH_CHARS[(h shr 20) % GLITCH_CHARS.length]
        return String(chars)
    }

    /** How far the red and cyan ghosts of NULL split apart, in text pixels. Jumps every tick. */
    fun glitchSplit(tick: Int, seed: Int): Float = 1f + (hash(tick, seed + 7) % 4)

    /** A sideways jolt of NULL in text pixels: most ticks none, some ticks a hard jump. */
    fun glitchJolt(tick: Int, seed: Int): Float {
        val h = hash(tick, seed + 13)
        return if (h % 100 < 25) ((h shr 8) % 9 - 4).toFloat() else 0f
    }

    /** A small non-negative hash, so glitches look random but tests can count on them. */
    private fun hash(tick: Int, seed: Int): Int {
        var x = tick * 0x45d9f3b + seed * 0x119de1f3
        x = (x xor (x ushr 16)) * 0x45d9f3b
        x = x xor (x ushr 16)
        return x and 0x7FFFFFFF
    }

    /** How many sparks a big hit bursts. */
    fun sparks(intensity: Float, crit: Boolean) = (8 + 14 * intensity + if (crit) 6 else 0).roundToInt()

    /** Opacity from 0 to 1, fading out over the last [FADE_TICKS] of [lifetime]. */
    fun alpha(age: Float, lifetime: Int = LIFETIME): Float = clamp((lifetime - age) / FADE_TICKS)

    /** Blocks risen at [age]: quick at first, then slowing down by the end of [lifetime]. */
    fun rise(age: Float, lifetime: Int = LIFETIME): Float {
        val t = clamp(age / lifetime)
        return 0.8f * (1f - (1f - t) * (1f - t))
    }

    /**
     * How much of the way from the camera to a number, [distance] blocks away, to draw it instead, pulling it
     * [clearance] blocks closer. Moving it
     * along the line of sight keeps it on the same spot on screen, and scaling it by the same amount keeps its
     * size, but it ends up in front of fire and the mob. Close numbers move at most halfway.
     */
    fun pull(distance: Double, clearance: Double = PULL): Double {
        if (distance <= 0) return 1.0
        return (distance - minOf(clearance, distance / 2)) / distance
    }

    /**
     * How far toward the camera to pull a number and its effects for a mob [width] blocks wide: at least
     * [PULL], and more for fat mobs, so the number, glows and particles all land in front of its body.
     */
    fun clearance(width: Double): Double = maxOf(PULL, width * 1.2 + 0.3)

    /** True if a hit [sinceLastHit] ticks after the previous one on the same entity joins its combo. */
    fun continuesCombo(sinceLastHit: Int) = sinceLastHit <= COMBO_TICKS

    /** Puts [alpha], from 0 to 1, into [color]. */
    fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha * 255).roundToInt().coerceIn(0, 255)
        return (a shl 24) or (color and 0xFFFFFF)
    }

    /** Blends two opaque colors, [t] = 0 giving [from]. */
    fun mix(from: Int, to: Int, t: Float): Int {
        fun channel(shift: Int): Int {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            return (a + (b - a) * t).roundToInt().coerceIn(0, 255) shl shift
        }
        return (0xFF shl 24) or channel(16) or channel(8) or channel(0)
    }

    private fun clamp(value: Float) = value.coerceIn(0f, 1f)

    private fun easeOutCubic(t: Float): Float {
        val u = 1f - t
        return 1f - u * u * u
    }

    /** Goes from 0 to 1, overshooting to about 1.1 on the way. */
    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val u = t - 1f
        return 1f + c3 * u * u * u + c1 * u * u
    }
}
