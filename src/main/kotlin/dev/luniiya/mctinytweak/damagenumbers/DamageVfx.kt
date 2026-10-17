package dev.luniiya.mctinytweak.damagenumbers

import dev.luniiya.mctinytweak.healthbar.HealthBars
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * The extra effects for really big hits, from 100 damage up. Each tier keeps everything from the tiers below:
 * - [HEAVY], 100 up: shockwave rings, a slash across the mob and a bass thump.
 * - [BRUTAL], 200 up: afterimages trailing the number, heart-colored dust and a heavier hit sound.
 * - [DEVASTATING], 500 up: light rays spinning behind the number, anime focus lines, and the number shatters.
 * - [MAX], 990 up: rainbow digits, a MAX tag, a starburst and an impact frame.
 *
 * Distances are in text pixels around the number and ages in ticks, like [DamageNumbers].
 */
object DamageVfx {
    const val NONE = 0
    const val HEAVY = 1
    const val BRUTAL = 2
    const val DEVASTATING = 3
    const val MAX = 4

    /** The least damage for each tier, by tier. */
    private val THRESHOLDS = floatArrayOf(0f, 100f, 200f, 500f, 990f)

    /** Ticks a ring takes to expand and fade. */
    const val RING_TICKS = 9

    /** Ticks between one ring and the next. */
    const val RING_GAP = 2

    /** Ticks a slash takes to cut across, then to thin out. */
    const val SLASH_IN = 2
    const val SLASH_OUT = 7

    /** Ticks between afterimages, and how long the trail lasts. */
    const val AFTERIMAGE_GAP = 1.2f
    const val AFTERIMAGE_TICKS = 14

    /** Ticks the rays take to grow in. */
    const val RAY_IN = 3

    /** Ticks the number takes to shatter at the end of its life. */
    const val SHATTER_TICKS = 10

    /** Ticks the starburst flares and collapses. */
    const val STAR_TICKS = 10

    /** The MAX tag pops in this many ticks after the hit. */
    const val MAX_TAG_DELAY = 3

    /** The impact frame: the world inverted, then a white flash fading out. */
    const val IMPACT_INVERT = 2f
    const val IMPACT_FLASH = 4f

    /** Ticks the focus lines stay. */
    const val FOCUS_TICKS = 12

    /** Every heart color in order, then gold, so a rainbow walks the whole health bar palette. */
    val RAINBOW = IntArray(HealthBars.PALETTE.size + 2) { i ->
        when (i) {
            0 -> HealthBars.layerColor(0)
            HealthBars.PALETTE.size + 1 -> HealthBars.GOLD
            else -> HealthBars.PALETTE[i - 1]
        }
    }

    /** Which tier [damage] reaches, from [NONE] to [MAX]. */
    fun tier(damage: Float): Int = THRESHOLDS.indexOfLast { damage >= it }

    /** Bigger tiers stay on screen longer, so their effects have time to play out. */
    fun lifetime(tier: Int): Int = DamageNumbers.LIFETIME + tier * 8

    /** How many shockwave rings a tier sends out. */
    fun rings(tier: Int): Int = tier

    /** The radius of ring [index] at [age], from 0 out to its full size, or null before it starts or after it ends. */
    fun ringRadius(age: Float, index: Int, tier: Int): Float? {
        val t = ringProgress(age, index) ?: return null
        val full = 34f + 10f * tier + 8f * index
        return full * easeOutCubic(t)
    }

    /** How thick ring [index] is: fat when it starts, thin as it spreads. */
    fun ringWidth(age: Float, index: Int): Float = (ringProgress(age, index)?.let { 7f * (1f - it) + 0.6f }) ?: 0f

    /** How visible ring [index] is. */
    fun ringAlpha(age: Float, index: Int): Float = (ringProgress(age, index)?.let { 1f - it * it }) ?: 0f

    private fun ringProgress(age: Float, index: Int): Float? {
        val t = (age - index * RING_GAP) / RING_TICKS
        return if (t < 0f || t >= 1f) null else t
    }

    /** How many slashes cut across the mob: one, then an X, then a star of cuts. */
    fun slashes(tier: Int): Int = when (tier) {
        NONE -> 0
        HEAVY -> 1
        BRUTAL -> 2
        DEVASTATING -> 3
        else -> 5
    }

    /** The angle of slash [index] in radians: the first one diagonal, the rest spread around it. */
    fun slashAngle(index: Int, count: Int, seed: Int): Float {
        val tilt = (Math.floorMod(seed, 40) - 20) / 180f * PI.toFloat()
        return (PI / 4).toFloat() + tilt + index * PI.toFloat() / count
    }

    /** How far a slash has cut, from 0 to 1. Later slashes in a tier come a tick after each other. */
    fun slashLength(age: Float, index: Int): Float = clamp((age - index * 0.7f) / SLASH_IN).let(::easeOutCubic)

    /** How thick a slash is: full once it has cut, then thinning to nothing. */
    fun slashWidth(age: Float, index: Int): Float {
        val since = age - index * 0.7f - SLASH_IN
        if (age < index * 0.7f) return 0f
        return if (since < 0f) 1f else 1f - clamp(since / SLASH_OUT)
    }

    /** How many afterimages trail the number. */
    fun afterimages(tier: Int): Int = if (tier < BRUTAL) 0 else 2 + 2 * (tier - BRUTAL)

    /** How strong afterimage [index] (1 is the closest) is at [age], fading as the number settles. */
    fun afterimageAlpha(age: Float, index: Int, count: Int): Float {
        if (index < 1 || index > count) return 0f
        return 0.55f * (1f - (index - 1f) / count) * (1f - clamp(age / AFTERIMAGE_TICKS))
    }

    /** How many light rays spin behind the number. */
    fun rays(tier: Int): Int = when {
        tier < DEVASTATING -> 0
        tier == DEVASTATING -> 10
        else -> 16
    }

    /** The angle of ray [index] at [age]: evenly spread and spinning. Odd [layer]s spin the other way. */
    fun rayAngle(index: Int, count: Int, age: Float, layer: Int = 0): Float {
        val spin = age * 0.09f * if (layer % 2 == 0) 1f else -1.4f
        return (index * 2 * PI / count).toFloat() + spin + layer * 0.2f
    }

    /** How long the rays are, growing in and then pulsing. */
    fun rayLength(age: Float, tier: Int): Float {
        val grow = easeOutBack(clamp(age / RAY_IN))
        val pulse = 1f + 0.12f * sin(age * 1.3f)
        return (46f + 18f * (tier - DEVASTATING)) * grow * pulse
    }

    /** How far through shattering the number is at [age], 0 until [SHATTER_TICKS] before the end, then up to 1. */
    fun shatter(age: Float, lifetime: Int): Float = clamp((age - (lifetime - SHATTER_TICKS)) / SHATTER_TICKS)

    /**
     * Where piece [index] of a shattered number has flown, in text pixels, and how far it has spun in radians.
     * Pieces burst outward, slow down, then fall.
     */
    fun shard(index: Int, progress: Float, seed: Int): Triple<Float, Float, Float> {
        val h = hash(index, seed)
        val angle = (h % 360) / 180f * PI.toFloat()
        val speed = 26f + (h shr 9) % 30
        val out = speed * easeOutCubic(progress)
        val fall = 40f * progress * progress
        val spin = ((h shr 15) % 9 - 4) * 1.1f * progress
        return Triple(cos(angle) * out, sin(angle) * out * 0.7f + fall, spin)
    }

    /** The color of digit [index] in a MAX hit: the whole palette cycling quickly across the digits. */
    fun rainbow(index: Int, age: Float): Int {
        val phase = index * 0.55f + age * 0.45f
        val i = floor(phase).toInt()
        return DamageNumbers.mix(RAINBOW[Math.floorMod(i, RAINBOW.size)], RAINBOW[Math.floorMod(i + 1, RAINBOW.size)], phase - i)
    }

    /** The MAX tag's size: nothing, then popping in big, then a bounce and a heartbeat pulse. */
    fun maxTagScale(age: Float): Float {
        val t = age - MAX_TAG_DELAY
        if (t <= 0f) return 0f
        val pop = 2.4f + (1f - 2.4f) * easeOutBack(clamp(t / 5f))
        return pop * (1f + 0.08f * sin(t * 1.6f))
    }

    /** The starburst's size from 0 to 1: a quick flare, then collapsing to a point. */
    fun starScale(age: Float): Float {
        if (age < 0f || age >= STAR_TICKS) return 0f
        return if (age < 2f) easeOutCubic(age / 2f) else 1f - easeInCubic((age - 2f) / (STAR_TICKS - 2f))
    }

    /** True while the impact frame inverts the world. */
    fun impactInverted(age: Float): Boolean = age in 0f..<IMPACT_INVERT

    /** How white the screen is after the inverted frame, fading out. */
    fun impactFlash(age: Float): Float {
        val t = age - IMPACT_INVERT
        return if (t < 0f || t >= IMPACT_FLASH) 0f else 0.75f * (1f - t / IMPACT_FLASH)
    }

    /** How visible the focus lines are: a hard snap in, then fading. */
    fun focusAlpha(age: Float): Float = if (age < 0f) 0f else 1f - clamp(age / FOCUS_TICKS)

    /** How many focus lines a tier draws. */
    fun focusLines(tier: Int): Int = if (tier < DEVASTATING) 0 else if (tier == DEVASTATING) 36 else 60

    /** Line [index]'s angle and how far in from the edge it reaches, from 0 to 1, flickering each tick. */
    fun focusLine(index: Int, count: Int, tick: Int, seed: Int): Pair<Float, Float> {
        val h = hash(index * 31 + tick, seed)
        val jitter = ((h % 100) / 100f - 0.5f) * 0.6f
        val angle = ((index + jitter) * 2 * PI / count).toFloat()
        return angle to 0.25f + ((h shr 8) % 100) / 100f * 0.35f
    }

    private fun hash(a: Int, b: Int): Int {
        var x = a * 0x45d9f3b + b * 0x119de1f3
        x = (x xor (x ushr 16)) * 0x45d9f3b
        x = x xor (x ushr 16)
        return x and 0x7FFFFFFF
    }

    private fun clamp(value: Float) = value.coerceIn(0f, 1f)

    private fun easeOutCubic(t: Float): Float {
        val u = 1f - t
        return 1f - u * u * u
    }

    private fun easeInCubic(t: Float) = t * t * t

    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val u = t - 1f
        return 1f + c3 * u * u * u + c1 * u * u
    }
}
