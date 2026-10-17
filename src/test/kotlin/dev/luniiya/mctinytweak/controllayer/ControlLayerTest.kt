package dev.luniiya.mctinytweak.controllayer

import com.google.gson.JsonParser
import dev.luniiya.mctinytweak.McTinyTweak
import net.fabricmc.loader.api.FabricLoader
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
    fun `heal level gives a huge positive heal without overflowing`() {
        val heal = 4 shl ControlCommands.HEAL_LEVEL
        assertTrue(heal > 1_000_000_000, "heal $heal")
        // 28 is the highest level that still heals; vanilla clamps the overflowed 29 to a heal of 0.
        assertTrue(4 shl (ControlCommands.HEAL_LEVEL + 1) <= 0)
        assertEquals("effect give @s minecraft:instant_health 1 28 true", ControlCommands.HEAL)
        assertEquals("kill 0-0-0-0-1", ControlCommands.kill("0-0-0-0-1"))
    }

    @Test
    fun `keyboard hook is applied and notices have text`() {
        val handler = Class.forName("net.minecraft.client.KeyboardHandler", false, javaClass.classLoader)
        assertTrue(handler.declaredMethods.any { "controlLayer" in it.name })
        val container = FabricLoader.getInstance().getModContainer(McTinyTweak.MOD_ID).orElseThrow()
        val lang = Files.newBufferedReader(container.findPath("assets/mc_tiny_tweak/lang/en_us.json").orElseThrow()).use { JsonParser.parseReader(it).asJsonObject }
        for (key in listOf("needs_op", "no_target")) assertTrue(lang.has("notice.mc_tiny_tweak.control.$key"), key)
    }
}
