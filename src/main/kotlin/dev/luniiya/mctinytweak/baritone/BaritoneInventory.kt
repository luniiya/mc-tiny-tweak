package dev.luniiya.mctinytweak.baritone

/**
 * Baritone's `allowInventory` makes it swap a throwaway block into hotbar slot 9 and a pickaxe into slot 1
 * every tick, even while it is idle. The mod only lets it while it has something to do.
 */
object BaritoneInventory {
    const val SETTING = "allowInventory"

    /** [preference] is what the player set; Baritone only gets it while the agent plays or Baritone is busy. */
    fun allowed(preference: Boolean, agentInControl: Boolean, baritoneBusy: Boolean) = preference && (agentInControl || baritoneBusy)
}
