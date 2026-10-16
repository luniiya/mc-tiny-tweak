package dev.luniiya.mctinytweak.mcp

import kotlin.math.atan2
import kotlin.math.sqrt

/** Name and angle helpers for the input tools, kept free of Minecraft types so they can be tested alone. */
object AgentControls {
    /** Short names an agent can use instead of key mapping ids. Any `key.*` id also works. */
    val KEY_ALIASES = mapOf(
        "forward" to "key.forward",
        "back" to "key.back",
        "left" to "key.left",
        "right" to "key.right",
        "jump" to "key.jump",
        "sneak" to "key.sneak",
        "sprint" to "key.sprint",
        "attack" to "key.attack",
        "use" to "key.use",
        "pick" to "key.pickItem",
        "drop" to "key.drop",
        "inventory" to "key.inventory",
        "swap_hands" to "key.swapOffhand",
        "chat" to "key.chat",
    )

    /** The longest a key can be held in one call: one minute. */
    const val MAX_HOLD_TICKS = 20 * 60

    fun mappingName(key: String): String? {
        val trimmed = key.trim()
        return KEY_ALIASES[trimmed.lowercase()] ?: trimmed.takeIf { it.startsWith("key.") }
    }

    data class Angles(val yaw: Float, val pitch: Float)

    /**
     * The yaw and pitch that look along ([dx], [dy], [dz]) in Minecraft's convention:
     * yaw 0 faces +Z (south) and grows clockwise, pitch is positive when looking down.
     */
    fun anglesTo(dx: Double, dy: Double, dz: Double): Angles {
        val horizontal = sqrt(dx * dx + dz * dz)
        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val pitch = Math.toDegrees(-atan2(dy, horizontal)).toFloat()
        return Angles(yaw, pitch.coerceIn(-90f, 90f))
    }
}
