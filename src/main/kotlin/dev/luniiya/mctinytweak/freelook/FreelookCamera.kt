package dev.luniiya.mctinytweak.freelook

import net.minecraft.client.CameraType

/**
 * The detached look direction used while freelook is held. Mouse movement turns this camera instead of the
 * player, so the player keeps moving and aiming where it was facing.
 */
class FreelookCamera {
    var active = false
        private set
    var yaw = 0f
        private set
    var pitch = 0f
        private set

    /** The perspective to restore when freelook ends. */
    var previousPerspective: CameraType? = null
        private set

    /** Starts from the player's current view and returns the perspective to switch to. */
    fun start(playerYaw: Float, playerPitch: Float, perspective: CameraType): CameraType {
        active = true
        yaw = playerYaw
        pitch = playerPitch
        previousPerspective = perspective
        return perspectiveWhileLooking(perspective)
    }

    /** Ends freelook and returns the perspective to go back to. */
    fun stop(): CameraType? {
        active = false
        return previousPerspective.also { previousPerspective = null }
    }

    /** Applies raw mouse deltas with the same scale and pitch limits vanilla uses for the player. */
    fun turn(dx: Double, dy: Double) {
        yaw += dx.toFloat() * MOUSE_SCALE
        pitch = (pitch + dy.toFloat() * MOUSE_SCALE).coerceIn(-MAX_PITCH, MAX_PITCH)
    }

    companion object {
        /** Matches `Entity.turn`. */
        const val MOUSE_SCALE = 0.15f
        const val MAX_PITCH = 90f

        /** First person switches to the back view so the player stays visible; other views are kept. */
        fun perspectiveWhileLooking(current: CameraType) =
            if (current == CameraType.FIRST_PERSON) CameraType.THIRD_PERSON_BACK else current
    }
}
