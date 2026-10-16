package dev.luniiya.mctinytweak.client.mcp

import baritone.api.BaritoneAPI
import net.fabricmc.loader.api.FabricLoader

/**
 * Talks to Baritone's API. Baritone is optional, so nothing here may run unless [installed] is true:
 * the JVM only loads Baritone's classes once one of these functions is called.
 */
object BaritoneBridge {
    val installed: Boolean get() = FabricLoader.getInstance().isModLoaded("baritone")

    /** Runs a Baritone command such as `mine diamond_ore`, without the `#` prefix. Client thread only. */
    fun execute(command: String): Boolean =
        BaritoneAPI.getProvider().primaryBaritone.commandManager.execute(command)

    /** Cancels pathing and every process. Client thread only. */
    fun stop() {
        BaritoneAPI.getProvider().primaryBaritone.pathingBehavior.cancelEverything()
    }

    /** The name of the process Baritone runs, such as Mine, or null when it is idle. Client thread only. */
    fun activeProcess(): String? =
        BaritoneAPI.getProvider().primaryBaritone.pathingControlManager.mostRecentInControl().map { it.displayName() }.orElse(null)

    /** What Baritone is doing right now. Client thread only. */
    fun status(): String {
        val baritone = BaritoneAPI.getProvider().primaryBaritone
        val pathing = baritone.pathingBehavior
        val process = baritone.pathingControlManager.mostRecentInControl().map { it.displayName() }.orElse("none")
        val goal = pathing.goal?.toString() ?: "none"
        val eta = pathing.estimatedTicksToGoal().map { "%.0f ticks".format(it) }.orElse("unknown")
        return "pathing: ${pathing.isPathing}\nprocess: $process\ngoal: $goal\nestimated time to goal: $eta"
    }
}
