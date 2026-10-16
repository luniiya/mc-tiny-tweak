package dev.luniiya.mctinytweak.mcp

import com.mojang.blaze3d.platform.InputConstants
import dev.luniiya.mctinytweak.client.McTinyTweakClient
import dev.luniiya.mctinytweak.client.mcp.AgentControl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TakeOverTest {
    private fun withAgent(block: () -> Unit) {
        val field = AgentControl::class.java.getDeclaredField("agent").apply { isAccessible = true }
        field.setBoolean(AgentControl, true)
        try {
            block()
        } finally {
            field.setBoolean(AgentControl, false)
        }
    }

    @Test
    fun `the player's keys only stop working while the agent is in control`() {
        val forward = InputConstants.getKey("key.keyboard.w")
        assertFalse(AgentControl.blocksKey(forward))
        withAgent {
            assertTrue(AgentControl.blocksKey(forward))
            assertTrue(AgentControl.blocksKey(InputConstants.getKey("key.mouse.left")))
        }
    }

    @Test
    fun `the mod menu key keeps working so the player can take control back`() {
        val menu = InputConstants.getKey(McTinyTweakClient.openMenuKey.saveString())
        withAgent { assertFalse(AgentControl.blocksKey(menu)) }
    }

    @Test
    fun `key, mouse and scroll input is hooked`() {
        fun handlers(name: String) = Class.forName(name, false, javaClass.classLoader).declaredMethods.map { it.name }
        val keys = handlers("net.minecraft.client.KeyMapping")
        for (handler in listOf("blockSet", "blockClick", "blockSetAll")) assertTrue(keys.any { it.endsWith(handler) }, handler)
        val mouse = handlers("net.minecraft.client.MouseHandler")
        for (handler in listOf("blockTurn", "blockScroll")) assertTrue(mouse.any { it.endsWith(handler) }, handler)
    }
}
