package dev.luniiya.mctinytweak.baritone

import com.google.gson.JsonParser
import dev.luniiya.mctinytweak.McTinyTweak
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class BaritoneMessagesTest {
    /** Built the way Baritone's Helper.logDirect builds a chat line. */
    private fun baritoneLine(tag: String, vararg text: String) = Component.literal("").apply {
        append(Component.literal(tag))
        append(Component.literal(" "))
        for (part in text) append(Component.literal(part))
    }

    @Test
    fun `route refusal feedback carries a danger reminder and Sol's cautionary tale`() {
        assertTrue(BaritoneMessages.routeRefused("Unable to find any path to obsidian, blacklisting presumably unreachable closest instance"))
        assertTrue(BaritoneMessages.routeRefused("UNABLE TO FIND A PATH"))
        assertFalse(BaritoneMessages.routeRefused("Mining obsidian"))
        assertTrue(BaritoneMessages.ROUTE_WARNING.contains("danger"))
        assertTrue(BaritoneMessages.ROUTE_WARNING.contains("Sol"))
        assertTrue(BaritoneMessages.ROUTE_WARNING.contains("lava delivery"))
        assertTrue(dev.luniiya.mctinytweak.mcp.McpProtocol.INSTRUCTIONS.contains("refused Baritone route"))
    }

    @Test
    fun `repeated route failures warn once per command in MCP feedback`() {
        val log = dev.luniiya.mctinytweak.client.baritone.BaritoneLog
        val chat = dev.luniiya.mctinytweak.client.mcp.Mcp.chat
        log.command("mine obsidian")
        val before = chat.lastId()
        repeat(3) { log.add(Component.literal("Unable to find any path to obsidian")) }
        assertEquals(1, chat.after(before, 50).count { it.text.contains("lava delivery") })
        log.command("goto 1 2 3")
        val next = chat.lastId()
        log.add(Component.literal("Unable to find any path"))
        assertEquals(1, chat.after(next, 50).count { it.text.contains("lava delivery") })
    }

    @Test
    fun `the Baritone tag is dropped from log lines`() {
        assertEquals("Mining iron_ore", BaritoneMessages.withoutTag(baritoneLine("[Baritone]", "Mining ", "iron_ore")).string)
        assertEquals("Done", BaritoneMessages.withoutTag(baritoneLine("[B]", "Done")).string)
    }

    @Test
    fun `lines without the tag are kept as they are`() {
        val plain = Component.literal("[Server] hello")
        assertSame(plain, BaritoneMessages.withoutTag(plain))
        val other = baritoneLine("[Someone]", "hi")
        assertSame(other, BaritoneMessages.withoutTag(other))
    }

    @Test
    fun `the Baritone log screen has its labels`() {
        val container = FabricLoader.getInstance().getModContainer(McTinyTweak.MOD_ID).orElseThrow()
        val lang = Files.newBufferedReader(container.findPath("assets/mc_tiny_tweak/lang/en_us.json").orElseThrow()).use { JsonParser.parseReader(it).asJsonObject }
        for (key in listOf("", ".tooltip", ".clear", ".empty")) assertTrue(lang.has("screen.mc_tiny_tweak.baritone_log$key"), key)
    }
}
