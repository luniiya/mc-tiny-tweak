package dev.luniiya.mctinytweak.mcp

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ContainerClicksTest {
    @Test
    fun `chest deposits and hotbar swaps use menu slot ids`() {
        assertDoesNotThrow { ContainerClicks.validate(3, 3, 27, 63, "quick_move", 0) }
        for (button in listOf(0, 8, 40)) assertDoesNotThrow { ContainerClicks.validate(0, 0, 9, 46, "swap", button) }
    }

    @Test
    fun `stale menus and slots are refused before sending packets`() {
        assertThrows(ToolError::class.java) { ContainerClicks.validate(3, 4, 0, 63, "pickup", 0) }
        for (slot in listOf(-1, 63, 1000)) assertThrows(ToolError::class.java) { ContainerClicks.validate(3, 3, slot, 63, "quick_move", 0) }
    }

    @Test
    fun `outside clicks only support pickup and action buttons are bounded`() {
        assertDoesNotThrow { ContainerClicks.validate(0, 0, -999, 46, "pickup", 0) }
        assertThrows(ToolError::class.java) { ContainerClicks.validate(0, 0, -999, 46, "throw", 0) }
        for ((action, button) in listOf("pickup" to 2, "swap" to 9, "throw" to 40, "quick_move" to -1, "clone" to 0)) {
            assertThrows(ToolError::class.java) { ContainerClicks.validate(0, 0, 9, 46, action, button) }
        }
    }
}
