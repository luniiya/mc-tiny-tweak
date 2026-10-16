package dev.luniiya.mctinytweak.freecam

import dev.luniiya.mctinytweak.freelook.FreelookCamera
import dev.luniiya.mctinytweak.mcp.AgentControls
import net.minecraft.util.Mth
import kotlin.math.atan2
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
    private var oldYaw = 0f
    private var oldPitch = 0f

    /** True while the camera circles the target like a spectator camera, until the player flies or turns. */
    var orbiting = false
        private set
    private var orbitAngle = 0.0

    fun start(x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        started = true
        this.x = x; this.y = y; this.z = z
        oldX = x; oldY = y; oldZ = z
        this.yaw = yaw
        this.pitch = pitch.coerceIn(-FreelookCamera.MAX_PITCH, FreelookCamera.MAX_PITCH)
        oldYaw = this.yaw
        oldPitch = this.pitch
        orbiting = false
    }

    /** Starts a little behind and above the given eyes, looking the same way, so the player is in view. */
    fun startBehind(eyeX: Double, eyeY: Double, eyeZ: Double, yaw: Float, pitch: Float) {
        val radians = Math.toRadians(yaw.toDouble())
        start(eyeX + sin(radians) * START_BACK, eyeY + START_UP, eyeZ - cos(radians) * START_BACK, yaw, pitch)
    }

    fun stop() {
        started = false
        orbiting = false
    }

    /** Starts circling a target at [targetX], [targetZ], from where the camera is now, so it glides in smoothly. */
    fun startOrbit(targetX: Double, targetZ: Double) {
        orbiting = true
        orbitAngle = atan2(z - targetZ, x - targetX)
    }

    fun stopOrbit() {
        orbiting = false
    }

    /**
     * One tick of the spectator camera: the circling spot moves on, and the camera eases toward it and toward
     * looking at the target's eyes, so the motion stays smooth even when the target moves suddenly.
     */
    fun orbitTick(
        targetX: Double,
        targetEyeY: Double,
        targetZ: Double,
        /** How far of the way from the eyes to the given spot is clear, from 0 to 1, so walls pull the camera in. */
        clear: (Double, Double, Double) -> Double = { _, _, _ -> 1.0 },
    ) {
        oldX = x; oldY = y; oldZ = z
        oldYaw = yaw; oldPitch = pitch
        orbitAngle += Math.toRadians(ORBIT_DEGREES_PER_TICK)
        val fullX = targetX + cos(orbitAngle) * ORBIT_RADIUS
        val fullY = targetEyeY + ORBIT_HEIGHT
        val fullZ = targetZ + sin(orbitAngle) * ORBIT_RADIUS
        val fraction = clear(fullX, fullY, fullZ).coerceIn(MIN_ORBIT_FRACTION, 1.0)
        val wantX = targetX + (fullX - targetX) * fraction
        val wantY = targetEyeY + (fullY - targetEyeY) * fraction
        val wantZ = targetZ + (fullZ - targetZ) * fraction
        x += (wantX - x) * ORBIT_EASE
        y += (wantY - y) * ORBIT_EASE
        z += (wantZ - z) * ORBIT_EASE
        val look = AgentControls.anglesTo(targetX - x, targetEyeY - y, targetZ - z)
        yaw += Mth.wrapDegrees(look.yaw - yaw) * LOOK_EASE
        pitch += (look.pitch - pitch) * LOOK_EASE
    }

    /** Same mouse scale and pitch limit as the player. Applies at once, without easing between ticks. */
    fun turn(dx: Double, dy: Double) {
        if (dx == 0.0 && dy == 0.0) return
        orbiting = false
        yaw += dx.toFloat() * FreelookCamera.MOUSE_SCALE
        pitch = (pitch + dy.toFloat() * FreelookCamera.MOUSE_SCALE).coerceIn(-FreelookCamera.MAX_PITCH, FreelookCamera.MAX_PITCH)
        oldYaw = yaw
        oldPitch = pitch
    }

    /**
     * Moves one tick. [forward] and [left] are -1, 0 or 1 and move in the look direction's horizontal plane,
     * [up] moves straight up or down, and [fast] multiplies the speed. Diagonals are not faster.
     */
    fun tick(forward: Float, left: Float, up: Float, fast: Boolean) {
        oldX = x; oldY = y; oldZ = z
        oldYaw = yaw; oldPitch = pitch
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
    fun yaw(partialTick: Float) = oldYaw + Mth.wrapDegrees(yaw - oldYaw) * partialTick
    fun pitch(partialTick: Float) = oldPitch + (pitch - oldPitch) * partialTick

    companion object {
        /** Blocks per tick. */
        const val SPEED = 0.5
        const val FAST_MULTIPLIER = 2.5
        const val START_BACK = 3.0
        const val START_UP = 1.0
        const val ORBIT_RADIUS = 5.0
        const val ORBIT_HEIGHT = 1.5
        /** One lap every 30 seconds. */
        const val ORBIT_DEGREES_PER_TICK = 0.6
        const val ORBIT_EASE = 0.08
        const val LOOK_EASE = 0.2f
        /** Never closer than this part of the orbit, so the camera does not end up inside the head. */
        const val MIN_ORBIT_FRACTION = 0.3

        /** Freecam is on while the player turned it on or an agent has taken over; off only when neither. */
        fun isOn(toggled: Boolean, agentInControl: Boolean) = toggled || agentInControl

        /** Whether the view shows the freecam, rather than the player's eyes. Watching the agent needs an agent. */
        fun shows(toggled: Boolean, agentInControl: Boolean, watchingAgent: Boolean) =
            isOn(toggled, agentInControl) && !(agentInControl && watchingAgent)
    }
}
