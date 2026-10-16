package dev.luniiya.mctinytweak.mcp

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AgentControlsTest {
    @Test
    fun `aliases and key mapping ids resolve, anything else does not`() {
        assertEquals("key.forward", AgentControls.mappingName("Forward"))
        assertEquals("key.attack", AgentControls.mappingName(" attack "))
        assertEquals("key.hotbar.1", AgentControls.mappingName("key.hotbar.1"))
        assertNull(AgentControls.mappingName("w"))
    }

    @Test
    fun `angles follow Minecraft's compass`() {
        fun yaw(dx: Double, dz: Double) = AgentControls.anglesTo(dx, 0.0, dz).yaw
        assertEquals(0f, yaw(0.0, 1.0), 0.01f, "south")
        assertEquals(90f, yaw(-1.0, 0.0), 0.01f, "west")
        assertEquals(-90f, yaw(1.0, 0.0), 0.01f, "east")
        assertEquals(180f, Math.abs(yaw(0.0, -1.0)), 0.01f, "north")
    }

    @Test
    fun `pitch is positive looking down and stays in range`() {
        assertEquals(45f, AgentControls.anglesTo(1.0, -1.0, 0.0).pitch, 0.01f)
        assertEquals(-90f, AgentControls.anglesTo(0.0, 5.0, 0.0).pitch, 0.01f)
        assertEquals(90f, AgentControls.anglesTo(0.0, -5.0, 0.0).pitch, 0.01f)
    }
}
