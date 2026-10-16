package dev.luniiya.mctinytweak.client.mcp

import baritone.api.BaritoneAPI
import dev.luniiya.mctinytweak.baritone.BaritoneInventory
import net.minecraft.client.Minecraft

/**
 * Applies [BaritoneInventory.allowed] to Baritone's live `allowInventory` value. The player's own value is
 * remembered and is what gets saved and shown, so the gate never changes the settings file.
 * Only call this when [BaritoneBridge.installed].
 */
object BaritoneInventoryGate {
    private val setting get() = BaritoneAPI.getSettings().allowInventory

    /** The player's value, or null before the first tick. */
    private var preference: Boolean? = null

    /** What the gate last wrote, to notice when something else (`#set`, the settings screen) changes it. */
    private var written: Boolean? = null

    fun tick(minecraft: Minecraft) {
        val now = setting.value
        if (written == null || now != written) preference = now
        val preference = preference ?: now
        val allowed = if (minecraft.player == null) preference else {
            val baritone = BaritoneAPI.getProvider().primaryBaritone
            val busy = baritone.pathingBehavior.isPathing || baritone.pathingControlManager.mostRecentInControl().isPresent
            BaritoneInventory.allowed(preference, AgentControl.agentInControl(), busy)
        }
        setting.value = allowed
        written = allowed
    }

    /** The value to show and save: the player's, not the gated one. */
    fun preference(): Boolean = preference ?: setting.value

    /** Runs [save] with the player's value in place, so the file keeps it. */
    fun <T> withPreference(save: () -> T): T {
        val gated = setting.value
        setting.value = preference()
        try {
            return save()
        } finally {
            setting.value = gated
        }
    }
}
