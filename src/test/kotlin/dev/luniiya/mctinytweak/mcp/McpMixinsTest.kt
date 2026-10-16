package dev.luniiya.mctinytweak.mcp

import com.google.gson.JsonParser
import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.mixin.client.mcp.KeyMappingAccessor
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.KeyMapping
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class McpMixinsTest {
    private fun hasHandler(className: String, handler: String) =
        Class.forName(className, false, javaClass.classLoader).declaredMethods.any { handler in it.name }

    @Test
    fun `chat lines, focus pausing and key clicks are hooked`() {
        assertTrue(KeyMappingAccessor::class.java.isAssignableFrom(KeyMapping::class.java))
        assertTrue(hasHandler("net.minecraft.client.gui.components.ChatComponent", "logForAgent"))
        assertTrue(hasHandler("net.minecraft.client.renderer.GameRenderer", "stayActiveForAgent"))
    }

    @Test
    fun `every MCP switch has a label and a tooltip`() {
        val container = FabricLoader.getInstance().getModContainer(McTinyTweak.MOD_ID).orElseThrow()
        val lang = Files.newBufferedReader(container.findPath("assets/mc_tiny_tweak/lang/en_us.json").orElseThrow()).use { JsonParser.parseReader(it).asJsonObject }
        for (feature in McpSettings.Feature.entries) {
            val key = "screen.mc_tiny_tweak.mcp.feature.${feature.key}"
            assertTrue(lang.has(key), key)
            assertTrue(lang.has("$key.tooltip"), "$key.tooltip")
        }
        for (notice in listOf("took_over", "released", "player_took_back", "action")) {
            assertTrue(lang.has("notice.mc_tiny_tweak.mcp.$notice"), notice)
        }
    }
}
