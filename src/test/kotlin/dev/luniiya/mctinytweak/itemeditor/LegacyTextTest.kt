package dev.luniiya.mctinytweak.itemeditor

import dev.luniiya.mctinytweak.MinecraftTestSupport
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Optional

class LegacyTextTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setUp() = MinecraftTestSupport.bootstrap()
    }

    /** Flattens a component into (text, effective style) runs. */
    private fun runs(component: Component): List<Pair<String, Style>> {
        val out = mutableListOf<Pair<String, Style>>()
        component.visit({ style, text ->
            if (text.isNotEmpty()) out += text to style
            Optional.empty<Unit>()
        }, Style.EMPTY)
        return out
    }

    @Test
    fun `plain text is kept and not italic`() {
        val parsed = LegacyText.parse("Hello")
        assertEquals("Hello", parsed.string)
        assertEquals(false, runs(parsed).single().second.isItalic)
    }

    @Test
    fun `color codes start a new run and formats stack on top`() {
        val result = runs(LegacyText.parse("&cRed &lBold"))
        assertEquals(listOf("Red ", "Bold"), result.map { it.first })
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), result[0].second.color)
        assertFalse(result[0].second.isBold)
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), result[1].second.color)
        assertTrue(result[1].second.isBold)
    }

    @Test
    fun `a color code clears earlier formats and reset clears color`() {
        val result = runs(LegacyText.parse("&lA&aB&rC"))
        assertTrue(result[0].second.isBold)
        assertFalse(result[1].second.isBold)
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GREEN), result[1].second.color)
        assertNull(result[2].second.color)
    }

    @Test
    fun `hex colors are parsed`() {
        val result = runs(LegacyText.parse("&#FF8800Orange"))
        assertEquals("Orange", result.single().first)
        assertEquals(0xFF8800, result.single().second.color!!.value)
    }

    @Test
    fun `double ampersand, unknown codes and a trailing ampersand stay literal`() {
        assertEquals("A&B &z end&", LegacyText.parse("A&&B &z end&").string)
        assertEquals("&#XYZ", LegacyText.parse("&#XYZ").string)
    }

    @Test
    fun `toLegacy writes codes that parse back to the same runs`() {
        for (input in listOf("Plain", "&cRed &lBold", "&lA&aB&rC", "&#123456hex&oit", "50&& off", "&a&l&nTriple")) {
            val parsed = LegacyText.parse(input)
            val legacy = LegacyText.toLegacy(parsed)
            assertEquals(runs(parsed), runs(LegacyText.parse(legacy)), "round trip of $input via $legacy")
        }
        assertEquals("&cHi", LegacyText.toLegacy(LegacyText.parse("&cHi")))
        assertEquals("50&& off", LegacyText.toLegacy(LegacyText.parse("50&& off")))
    }

    @Test
    fun `toLegacy reads styles from components not made by the parser`() {
        val component = Component.literal("Gold").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        assertEquals("&6&lGold", LegacyText.toLegacy(component))
        assertEquals("Plain", LegacyText.toLegacy(Component.literal("Plain")))
    }
}
