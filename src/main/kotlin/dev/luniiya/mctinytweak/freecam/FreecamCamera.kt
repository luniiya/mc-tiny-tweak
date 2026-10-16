package dev.luniiya.mctinytweak.freecam

import dev.luniiya.mctinytweak.freelook.FreelookCamera
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A camera that flies on its own, away from the player. It moves once per tick and is interpolated between
 * ticks, like an entity, so it looks smooth at any frame rate.
 */
class FreecamCamera {
    /** True once placed; it keeps its spot while hidden, for example while watching through the agent's eyes. */
    var started = false
        private set
    var x = 0.0
        private set
    var y = 0.0
        private set
    var z = 0.0
        private set
    private var oldX = 0.0
    private var oldY = 0.0
    private var oldZ = 0.0
    var yaw = 0f
        private set
    var pitch = 0f
        private set

    fun start(x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        started = true
        this.x = x; this.y = y; this.z = z
        oldX = x; oldY = y; oldZ = z
        this.yaw = yaw
        this.pitch = pitch.coerceIn(-FreelookCamera.MAX_PITCH, FreelookCamera.MAX_PITCH)
    }

    /** Starts a little behind and above the given eyes, looking the same way, so the player is in view. */
    fun startBehind(eyeX: Double, eyeY: Double, eyeZ: Double, yaw: Float, pitch: Float) {
        val radians = Math.toRadians(yaw.toDouble())
        start(eyeX + sin(radians) * START_BACK, eyeY + START_UP, eyeZ - cos(radians) * START_BACK, yaw, pitch)
    }

    fun stop() {
        started = false
    }

    /** Same mouse scale and pitch limit as the player. */
    fun turn(dx: Double, dy: Double) {
        yaw += dx.toFloat() * FreelookCamera.MOUSE_SCALE
        pitch = (pitch + dy.toFloat() * FreelookCamera.MOUSE_SCALE).coerceIn(-FreelookCamera.MAX_PITCH, FreelookCamera.MAX_PITCH)
    }

    /**
     * Moves one tick. [forward] and [left] are -1, 0 or 1 and move in the look direction's horizontal plane,
     * [up] moves straight up or down, and [fast] multiplies the speed. Diagonals are not faster.
     */
    fun tick(forward: Float, left: Float, up: Float, fast: Boolean) {
        oldX = x; oldY = y; oldZ = z
        val speed = if (fast) SPEED * FAST_MULTIPLIER else SPEED
        val length = sqrt(forward * forward + left * left)
        if (length > 0f) {
            val f = forward / length
            val l = left / length
            val radians = Math.toRadians(yaw.toDouble())
            val sin = sin(radians)
            val cos = cos(radians)
            // Yaw 0 looks south (+z); the player's left is then east (+x).
            x += (-sin * f + cos * l) * speed
            z += (cos * f + sin * l) * speed
        }
        y += up.coerceIn(-1f, 1f) * speed
    }

    fun x(partialTick: Float) = oldX + (x - oldX) * partialTick
    fun y(partialTick: Float) = oldY + (y - oldY) * partialTick
    fun z(partialTick: Float) = oldZ + (z - oldZ) * partialTick

    companion object {
        /** Blocks per tick. */
        const val SPEED = 0.5
        const val FAST_MULTIPLIER = 2.5
        const val START_BACK = 3.0
        const val START_UP = 1.0

        /** Freecam is on while the player turned it on or an agent has taken over; off only when neither. */
        fun isOn(toggled: Boolean, agentInControl: Boolean) = toggled || agentInControl

        /** Whether the view shows the freecam, rather than the player's eyes. Watching the agent needs an agent. */
        fun shows(toggled: Boolean, agentInControl: Boolean, watchingAgent: Boolean) =
            isOn(toggled, agentInControl) && !(agentInControl && watchingAgent)
    }
}
