package dev.luniiya.mctinytweak.controllayer

import com.google.gson.JsonParser
import dev.luniiya.mctinytweak.McTinyTweak
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.world.level.GameType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.lwjgl.glfw.GLFW.*
import java.nio.file.Files

class ControlLayerTest {
    private val layer = ControlLayer()

    @Test
    fun `ctrl H heals and ctrl K kills, and both are hidden from the game`() {
        assertEquals(ControlLayer.Result(ControlLayer.Action.HEAL, true), layer.onKey(GLFW_KEY_H, GLFW_PRESS, true, false))
        assertEquals(ControlLayer.Result(ControlLayer.Action.KILL, true), layer.onKey(GLFW_KEY_K, GLFW_PRESS, true, false))
    }

    @Test
    fun `ctrl G swaps between spectator and survival`() {
        assertEquals(ControlLayer.Result(ControlLayer.Action.SPECTATOR, true), layer.onKey(GLFW_KEY_G, GLFW_PRESS, true, false))
        assertEquals("gamemode spectator", ControlCommands.toggleSpectator(GameType.SURVIVAL).command)
        assertEquals("gamemode spectator", ControlCommands.toggleSpectator(GameType.CREATIVE).command)
        assertEquals("gamemode survival", ControlCommands.toggleSpectator(GameType.SPECTATOR).command)
        assertEquals("spectator", ControlCommands.toggleSpectator(null).notice)
    }

    @Test
    fun `keys without ctrl, other keys and keys on a screen pass through`() {
        assertEquals(ControlLayer.Result.PASS, layer.onKey(GLFW_KEY_H, GLFW_PRESS, false, false))
        assertEquals(ControlLayer.Result.PASS, layer.onKey(GLFW_KEY_J, GLFW_PRESS, true, false))
        assertEquals(ControlLayer.Result.PASS, layer.onKey(GLFW_KEY_H, GLFW_PRESS, true, true))
        assertEquals(ControlLayer.Result.PASS, layer.onKey(GLFW_KEY_H, GLFW_RELEASE, false, false))
    }

    @Test
    fun `holding the key repeats nothing and its release stays hidden even after ctrl is let go`() {
        layer.onKey(GLFW_KEY_H, GLFW_PRESS, true, false)
        assertEquals(ControlLayer.Result.SWALLOW, layer.onKey(GLFW_KEY_H, GLFW_REPEAT, true, false))
        assertEquals(ControlLayer.Result.SWALLOW, layer.onKey(GLFW_KEY_H, GLFW_REPEAT, false, false))
        assertEquals(ControlLayer.Result.SWALLOW, layer.onKey(GLFW_KEY_H, GLFW_RELEASE, false, false))
        // Once released, plain H belongs to the game again.
        assertEquals(ControlLayer.Result.PASS, layer.onKey(GLFW_KEY_H, GLFW_PRESS, false, false))
    }

    @Test
    fun `every shortcut has its own key`() {
        val keys = ControlLayer.Action.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `ctrl H turns infinite healing on, then clears it`() {
        val on = ControlCommands.toggleHealing(active = false)
        assertEquals("effect give @s minecraft:instant_health infinite 0 true", on.command)
        assertEquals("healing.on", on.notice)
        val off = ControlCommands.toggleHealing(active = true)
        assertEquals("effect clear @s minecraft:instant_health", off.command)
        assertEquals("healing.off", off.notice)
        assertEquals("kill 0-0-0-0-1", ControlCommands.kill("0-0-0-0-1").command)
    }

    @Test
    fun `keyboard hook is applied and every shortcut has a notification`() {
        val handler = Class.forName("net.minecraft.client.KeyboardHandler", false, javaClass.classLoader)
        assertTrue(handler.declaredMethods.any { "controlLayer" in it.name })
        val container = FabricLoader.getInstance().getModContainer(McTinyTweak.MOD_ID).orElseThrow()
        val lang = Files.newBufferedReader(container.findPath("assets/mc_tiny_tweak/lang/en_us.json").orElseThrow()).use { JsonParser.parseReader(it).asJsonObject }
        val steps = listOf(true, false).map(ControlCommands::toggleHealing) +
            listOf(GameType.SURVIVAL, GameType.SPECTATOR).map(ControlCommands::toggleSpectator) + ControlCommands.kill("x")
        for (key in listOf("needs_op", "no_target") + steps.map { it.notice }) assertTrue(lang.has("notice.mc_tiny_tweak.control.$key"), key)
        // Each command hides its own vanilla success line, which must be a real vanilla message.
        val vanilla = net.minecraft.locale.Language.getInstance()
        for (step in steps) assertTrue(vanilla.has(step.hiddenFeedback), step.hiddenFeedback)
    }
}
