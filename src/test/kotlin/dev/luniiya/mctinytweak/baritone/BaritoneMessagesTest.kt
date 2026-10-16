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
