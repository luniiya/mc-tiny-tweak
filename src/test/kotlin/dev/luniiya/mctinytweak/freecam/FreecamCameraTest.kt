package dev.luniiya.mctinytweak.freecam

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FreecamCameraTest {
    private fun camera(yaw: Float = 0f) = FreecamCamera().apply { start(0.0, 64.0, 0.0, yaw, 0f) }

    @Test
    fun `freecam is on when the toggle or the agent is on, and off only when both are off`() {
        assertFalse(FreecamCamera.isOn(toggled = false, agentInControl = false))
        assertTrue(FreecamCamera.isOn(toggled = true, agentInControl = false))
        assertTrue(FreecamCamera.isOn(toggled = false, agentInControl = true))
        assertTrue(FreecamCamera.isOn(toggled = true, agentInControl = true))
    }

    @Test
    fun `watching the agent shows its eyes only while an agent plays`() {
        assertFalse(FreecamCamera.shows(toggled = false, agentInControl = true, watchingAgent = true))
        assertFalse(FreecamCamera.shows(toggled = true, agentInControl = true, watchingAgent = true))
        assertTrue(FreecamCamera.shows(toggled = true, agentInControl = false, watchingAgent = true))
        assertTrue(FreecamCamera.shows(toggled = false, agentInControl = true, watchingAgent = false))
        assertFalse(FreecamCamera.shows(toggled = false, agentInControl = false, watchingAgent = false))
    }

    @Test
    fun `forward follows the look direction like the player`() {
        val south = camera(0f).apply { tick(1f, 0f, 0f, false) }
        assertEquals(FreecamCamera.SPEED, south.z, 1e-9)
        assertEquals(0.0, south.x, 1e-9)
        val west = camera(90f).apply { tick(1f, 0f, 0f, false) }
        assertEquals(-FreecamCamera.SPEED, west.x, 1e-9)
        assertEquals(0.0, west.z, 1e-9)
    }

    @Test
    fun `left strafes to the player's left`() {
        val c = camera(0f).apply { tick(0f, 1f, 0f, false) }
        assertEquals(FreecamCamera.SPEED, c.x, 1e-9, "facing south, left is east")
    }

    @Test
    fun `diagonals are not faster and sprint multiplies the speed`() {
        val c = camera(0f).apply { tick(1f, 1f, 0f, false) }
        assertEquals(FreecamCamera.SPEED, Math.hypot(c.x, c.z), 1e-6)
        val fast = camera(0f).apply { tick(1f, 0f, 0f, true) }
        assertEquals(FreecamCamera.SPEED * FreecamCamera.FAST_MULTIPLIER, fast.z, 1e-9)
    }

    @Test
    fun `up and down move straight, ignoring pitch`() {
        val c = camera().apply { turn(0.0, 400.0); tick(0f, 0f, 1f, false) }
        assertEquals(64.0 + FreecamCamera.SPEED, c.y, 1e-9)
        assertEquals(0.0, c.x, 1e-9)
        assertEquals(0.0, c.z, 1e-9)
    }

    @Test
    fun `positions are interpolated between ticks`() {
        val c = camera(0f).apply { tick(1f, 0f, 0f, false) }
        assertEquals(FreecamCamera.SPEED / 2, c.z(0.5f), 1e-9)
        assertEquals(0.0, c.z(0f), 1e-9)
    }

    @Test
    fun `starts behind and above the player so it is not inside their head`() {
        val south = FreecamCamera().apply { startBehind(10.0, 70.0, 10.0, 0f, 20f) }
        assertEquals(10.0 - FreecamCamera.START_BACK, south.z, 1e-9, "facing south, behind is north")
        assertEquals(10.0, south.x, 1e-9)
        assertEquals(70.0 + FreecamCamera.START_UP, south.y, 1e-9)
        assertEquals(20f, south.pitch)
        val west = FreecamCamera().apply { startBehind(0.0, 0.0, 0.0, 90f, 0f) }
        assertEquals(FreecamCamera.START_BACK, west.x, 1e-9, "facing west, behind is east")
    }

    @Test
    fun `the spectator camera circles the target at its distance and looks at it`() {
        val c = FreecamCamera().apply { start(5.0, 70.0, 0.0, 0f, 0f); startOrbit(0.0, 0.0) }
        assertTrue(c.orbiting)
        repeat(400) { c.orbitTick(0.0, 65.0, 0.0) }
        assertEquals(FreecamCamera.ORBIT_RADIUS, Math.hypot(c.x, c.z), 0.3)
        assertEquals(65.0 + FreecamCamera.ORBIT_HEIGHT, c.y, 0.05)
        val look = dev.luniiya.mctinytweak.mcp.AgentControls.anglesTo(-c.x, 65.0 - c.y, -c.z)
        assertEquals(0f, net.minecraft.util.Mth.wrapDegrees(look.yaw - c.yaw), 3f, "faces the target")
        assertEquals(look.pitch, c.pitch, 3f)
    }

    @Test
    fun `walls pull the spectator camera in, but not into the head`() {
        val walled = FreecamCamera().apply { start(2.5, 66.5, 0.0, 0f, 0f); startOrbit(0.0, 0.0) }
        repeat(400) { walled.orbitTick(0.0, 65.0, 0.0) { _, _, _ -> 0.5 } }
        assertEquals(FreecamCamera.ORBIT_RADIUS * 0.5, Math.hypot(walled.x, walled.z), 0.3)
        val buried = FreecamCamera().apply { start(2.5, 66.5, 0.0, 0f, 0f); startOrbit(0.0, 0.0) }
        repeat(400) { buried.orbitTick(0.0, 65.0, 0.0) { _, _, _ -> 0.0 } }
        assertEquals(FreecamCamera.ORBIT_RADIUS * FreecamCamera.MIN_ORBIT_FRACTION, Math.hypot(buried.x, buried.z), 0.3)
    }

    @Test
    fun `the spectator camera eases instead of jumping when the target teleports`() {
        val c = FreecamCamera().apply { start(5.0, 70.0, 0.0, 0f, 0f); startOrbit(0.0, 0.0) }
        c.orbitTick(100.0, 65.0, 0.0)
        assertTrue(c.x < 20.0, "moved only part of the way: ${c.x}")
    }

    @Test
    fun `turning the mouse ends the spectator camera, and so does a fresh start`() {
        val c = FreecamCamera().apply { start(5.0, 70.0, 0.0, 0f, 0f); startOrbit(0.0, 0.0) }
        c.turn(0.0, 0.0)
        assertTrue(c.orbiting, "no movement keeps it going")
        c.turn(3.0, 0.0)
        assertFalse(c.orbiting)
        c.startOrbit(0.0, 0.0)
        c.start(0.0, 0.0, 0.0, 0f, 0f)
        assertFalse(c.orbiting)
    }

    @Test
    fun `yaw is interpolated the short way around`() {
        val c = FreecamCamera().apply { start(5.0, 70.0, 0.0, 179f, 0f); startOrbit(0.0, 0.0) }
        c.orbitTick(0.0, 70.0, 0.0)
        val halfway = c.yaw(0.5f)
        assertTrue(kotlin.math.abs(net.minecraft.util.Mth.wrapDegrees(halfway - 179f)) <= kotlin.math.abs(net.minecraft.util.Mth.wrapDegrees(c.yaw - 179f)) + 0.01f)
    }

    @Test
    fun `turning clamps the pitch`() {
        val c = camera().apply { turn(100.0, 10_000.0) }
        assertEquals(90f, c.pitch)
        assertEquals(15f, c.yaw, 1e-4f)
    }

    @Test
    fun `stopping forgets the spot so the next start begins at the player`() {
        val c = camera()
        assertTrue(c.started)
        c.stop()
        assertFalse(c.started)
    }
}
