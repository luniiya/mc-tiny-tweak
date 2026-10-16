package dev.luniiya.mctinytweak.freecam

import com.google.gson.JsonParser
import com.mojang.blaze3d.platform.InputConstants
import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.client.McTinyTweakClient
import dev.luniiya.mctinytweak.client.freecam.Freecam
import dev.luniiya.mctinytweak.client.mcp.AgentControl
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.fabricmc.loader.api.FabricLoader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class FreecamHooksTest {
    private fun hasHandler(className: String, handler: String) =
        Class.forName(className, false, javaClass.classLoader).declaredMethods.any { it.name.endsWith(handler) }

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
    fun `the camera, hand and crosshair follow the freecam`() {
        assertTrue(hasHandler("net.minecraft.client.Camera", "useFreecam"))
        assertTrue(hasHandler("net.minecraft.client.renderer.GameRenderer", "hideHandInFreecam"))
        assertTrue(hasHandler("net.minecraft.client.gui.Gui", "hideCrosshairInFreecam"))
    }

    @Test
    fun `agent screens keep the player's mouse, keys and view`() {
        assertTrue(hasHandler("net.minecraft.client.MouseHandler", "keepMouseForFreecam"))
        assertTrue(hasHandler("net.minecraft.client.MouseHandler", "ignoreClicksOnAgentScreen"))
        assertTrue(hasHandler("net.minecraft.client.KeyboardHandler", "keepKeysFromAgentScreen"))
        assertTrue(hasHandler("net.minecraft.client.KeyboardHandler", "keepTypingFromAgentScreen"))
        assertTrue(hasHandler("net.minecraft.client.renderer.GameRenderer", "drawAgentScreen"))
        assertTrue(hasHandler("net.minecraft.client.gui.screens.Screen", "skipPanelBackground"))
    }

    @Test
    fun `only containers count as the agent's screen, and only while it plays`() {
        assertFalse(dev.luniiya.mctinytweak.client.freecam.AgentScreen.open(null))
        withAgent { assertFalse(dev.luniiya.mctinytweak.client.freecam.AgentScreen.open(null)) }
    }

    @Test
    fun `the freecam and agent view keys keep working while the agent plays`() {
        withAgent {
            for (mapping in listOf(McTinyTweakClient.freecamKey, McTinyTweakClient.agentViewKey)) {
                assertFalse(AgentControl.blocksKey(InputConstants.getKey(mapping.saveString())), mapping.name)
            }
        }
    }

    @Test
    fun `freecam hides the hotbar and the player's bars`() {
        val hidden = dev.luniiya.mctinytweak.client.freecam.FreecamHud.HIDDEN
        for (id in listOf(VanillaHudElements.HOTBAR, VanillaHudElements.HEALTH_BAR, VanillaHudElements.FOOD_BAR, VanillaHudElements.EXPERIENCE_LEVEL)) {
            assertTrue(id in hidden, id.toString())
        }
        assertFalse(VanillaHudElements.CHAT in hidden, "chat stays so agent actions are visible")
    }

    @Test
    fun `nothing is shown before freecam is placed`() {
        assertFalse(Freecam.shown())
        assertFalse(Freecam.holdsPlayerInput())
    }

    @Test
    fun `keys, the menu toggle and the status card have text`() {
        val container = FabricLoader.getInstance().getModContainer(McTinyTweak.MOD_ID).orElseThrow()
        val lang = Files.newBufferedReader(container.findPath("assets/mc_tiny_tweak/lang/en_us.json").orElseThrow()).use { JsonParser.parseReader(it).asJsonObject }
        val keys = listOf(
            McTinyTweakClient.freecamKey.name, McTinyTweakClient.agentViewKey.name,
            "screen.mc_tiny_tweak.freecam", "screen.mc_tiny_tweak.freecam.tooltip",
            "notice.mc_tiny_tweak.freecam.on", "notice.mc_tiny_tweak.freecam.off",
        ) + listOf("title", "last", "baritone", "vitals", "to_agent", "to_freecam").map { "hud.mc_tiny_tweak.agent.$it" }
        for (key in keys) assertTrue(lang.has(key), key)
    }
}
