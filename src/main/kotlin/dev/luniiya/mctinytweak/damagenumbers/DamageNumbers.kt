package dev.luniiya.mctinytweak.damagenumbers

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Floating damage numbers, worked out on the client from the health the server already syncs for every
 * living entity: when an entity's health drops between two ticks, the drop becomes a number.
 */
object DamageNumbers {
    /** Ticks a number stays on screen. */
    const val LIFETIME = 24

    /** Ticks the pop-in takes, starting bigger and settling to normal size. */
    const val POP_TICKS = 4

    /** Ticks of fading out at the end. */
    const val FADE_TICKS = 8

    /** How long after a critical hit animation a health drop still counts as a crit. */
    const val CRIT_WINDOW = 2

    /** Drops smaller than this are rounding noise. */
    private const val MIN_DAMAGE = 0.05f

    const val WHITE = 0xFFFFFFFF.toInt()
    const val YELLOW = 0xFFFFE040.toInt()
    const val ORANGE = 0xFFFF9020.toInt()
    const val RED = 0xFFFF3030.toInt()
    const val CRIT = 0xFFFFC820.toInt()

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

    /** Remembers when entities showed a critical hit animation, which the server sends just before the damage. */
    class Crits {
        private val at = HashMap<Int, Long>()

        fun mark(id: Int, time: Long) {
            at[id] = time
        }

        /** True if [id] showed a crit within [CRIT_WINDOW] ticks before [time]. Each crit is only used once. */
        fun take(id: Int, time: Long): Boolean {
            val crit = at.remove(id) ?: return false
            return time - crit in 0..CRIT_WINDOW
        }

        fun prune(time: Long) {
            at.values.removeIf { time - it > CRIT_WINDOW }
        }

        fun clear() = at.clear()
    }

    /** `6` for whole numbers and anything from 10 up, otherwise one decimal like `4.5`. */
    fun format(damage: Float): String {
        val tenths = (damage * 10).roundToInt()
        return if (damage >= 10 || tenths % 10 == 0) (tenths / 10.0).roundToInt().toString() else "${tenths / 10}.${tenths % 10}"
    }

    /** Bigger hits get warmer colors, and crits always show gold. */
    fun color(damage: Float, crit: Boolean): Int = when {
        crit -> CRIT
        damage >= 20 -> RED
        damage >= 10 -> ORANGE
        damage >= 4 -> YELLOW
        else -> WHITE
    }

    /** Size multiplier: pops in big, settles to 1, and crits stay a bit bigger. */
    fun scale(age: Float, crit: Boolean): Float {
        val base = if (crit) 1.35f else 1f
        val pop = 1f - min(max(age / POP_TICKS, 0f), 1f)
        return base * (1f + 0.6f * pop)
    }

    /** Opacity from 0 to 1, fading out over the last [FADE_TICKS]. */
    fun alpha(age: Float): Float = min(max((LIFETIME - age) / FADE_TICKS, 0f), 1f)

    /** Blocks risen at [age]: quick at first, then slowing down. */
    fun rise(age: Float): Float {
        val t = min(max(age / LIFETIME, 0f), 1f)
        return 0.8f * (1f - (1f - t) * (1f - t))
    }

    /** Puts [alpha], from 0 to 1, into [color]. */
    fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha * 255).roundToInt().coerceIn(0, 255)
        return (a shl 24) or (color and 0xFFFFFF)
    }
}
