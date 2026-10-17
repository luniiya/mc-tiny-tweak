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

/**
 * One shortcut run: the vanilla OP command (without the slash), the vanilla success line it hides,
 * and the notification shown in its place.
 */
data class ControlStep(val command: String, val hiddenFeedback: String, val notice: String)

/** Vanilla OP commands the Ctrl layer sends. */
object ControlCommands {
    /**
     * An infinite instant effect never runs out and heals 4 HP every tick (instant effects tick while they last),
     * so you only die to a one-shot. Clearing the effect turns it off. `true` hides the particles.
     */
    fun toggleHealing(active: Boolean) =
        if (active) ControlStep("effect clear @s minecraft:instant_health", "commands.effect.clear.specific.success.single", "healing.off")
        else ControlStep("effect give @s minecraft:instant_health infinite 0 true", "commands.effect.give.success.single", "healing.on")

    fun kill(uuid: String) = ControlStep("kill $uuid", "commands.kill.success.single", "killed")

    /** Spectator goes back to survival; any other mode goes to spectator. */
    fun toggleSpectator(current: GameType?) =
        if (current == GameType.SPECTATOR) ControlStep("gamemode survival", "commands.gamemode.success.self", "survival")
        else ControlStep("gamemode spectator", "commands.gamemode.success.self", "spectator")
}
