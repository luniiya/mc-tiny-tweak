package dev.luniiya.mctinytweak.baritone

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BaritoneInventoryTest {
    @Test
    fun `an idle Baritone never touches the hotbar, even with allowInventory on`() {
        // Regression: idle Baritone kept moving cobblestone into hotbar slot 9 with no agent playing.
        assertFalse(BaritoneInventory.allowed(preference = true, agentInControl = false, baritoneBusy = false))
    }

    @Test
    fun `the agent or a running Baritone task gets the inventory when the player allows it`() {
        assertTrue(BaritoneInventory.allowed(preference = true, agentInControl = true, baritoneBusy = false))
        assertTrue(BaritoneInventory.allowed(preference = true, agentInControl = false, baritoneBusy = true))
        assertFalse(BaritoneInventory.allowed(preference = false, agentInControl = true, baritoneBusy = true))
    }
}
