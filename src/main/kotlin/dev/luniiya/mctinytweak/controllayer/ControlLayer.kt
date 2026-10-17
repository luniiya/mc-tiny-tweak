package dev.luniiya.mctinytweak.controllayer

import net.minecraft.world.level.GameType
import org.lwjgl.glfw.GLFW

/**
 * Ctrl + key shortcuts that belong to the mod alone. A key pressed with Ctrl that has a shortcut here
 * never reaches vanilla key mappings or other mods: its press, repeats and release are all swallowed.
 */
class ControlLayer {
    enum class Action(val key: Int, val id: String) {
        HEAL(GLFW.GLFW_KEY_H, "heal"),
        KILL(GLFW.GLFW_KEY_K, "kill"),
        SPECTATOR(GLFW.GLFW_KEY_G, "spectator"),
    }

    /** What to do with one key event: maybe run an action, and whether to hide the event from the game. */
    data class Result(val run: Action?, val consume: Boolean) {
        companion object {
            val PASS = Result(null, false)
            val SWALLOW = Result(null, true)
        }
    }

    /** Keys whose press was swallowed, so their release is swallowed too even if Ctrl is let go first. */
    private val held = HashSet<Int>()

    /** [action] is GLFW's: 0 release, 1 press, 2 repeat. Only works in the world, not on screens. */
    fun onKey(key: Int, action: Int, ctrlDown: Boolean, screenOpen: Boolean): Result {
        if (action == GLFW.GLFW_RELEASE) return if (held.remove(key)) Result.SWALLOW else Result.PASS
        if (key in held) return Result.SWALLOW
        if (!ctrlDown || screenOpen) return Result.PASS
        val shortcut = Action.entries.firstOrNull { it.key == key } ?: return Result.PASS
        held += key
        return Result(if (action == GLFW.GLFW_PRESS) shortcut else null, true)
    }
}

/** Vanilla OP commands the Ctrl layer sends, without the leading slash. */
object ControlCommands {
    /**
     * Instant Health heals 4 shl level, so level 28 is about a billion health: full HP even with boosted max health.
     * It is also the highest level that works: 29 and up overflow, and vanilla clamps that to a heal of 0.
     * `true` hides the particles.
     */
    const val HEAL_LEVEL = 28
    const val HEAL = "effect give @s minecraft:instant_health 1 $HEAL_LEVEL true"

    fun kill(uuid: String) = "kill $uuid"

    /** Spectator goes back to survival; any other mode goes to spectator. */
    fun toggleSpectator(current: GameType?) =
        if (current == GameType.SPECTATOR) "gamemode survival" else "gamemode spectator"
}
