package dev.luniiya.mctinytweak.attributes

import com.mojang.brigadier.StringReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AttributeCommandsTest {
    @Test
    fun `set and reset target only yourself`() {
        assertEquals("attribute @s minecraft:scale base set 2.5", AttributeCommands.setBase("minecraft:scale", 2.5))
        assertEquals("attribute @s minecraft:scale base reset", AttributeCommands.resetBase("minecraft:scale"))
    }

    @Test
    fun `huge values are written without an exponent so the command parser reads them`() {
        val text = AttributeCommands.format(6.0E7)
        assertEquals("60000000", text)
        assertEquals(6.0E7, StringReader(text).readDouble())
        assertEquals(1.0E-4, StringReader(AttributeCommands.format(1.0E-4)).readDouble())
    }

    @Test
    fun `whole numbers drop the trailing zero`() {
        assertEquals("20", AttributeCommands.format(20.0))
        assertEquals("0", AttributeCommands.format(-0.0))
        assertEquals("-1.5", AttributeCommands.format(-1.5))
    }

    @Test
    fun `typed values must be numbers inside the range`() {
        assertEquals(4.0, AttributeCommands.parse(" 4 ", 0.0, 1024.0))
        assertNull(AttributeCommands.parse("abc", 0.0, 1024.0))
        assertNull(AttributeCommands.parse("2000", 0.0, 1024.0))
        assertNull(AttributeCommands.parse("NaN", 0.0, 1024.0))
        assertNull(AttributeCommands.parse("", 0.0, 1024.0))
    }

    @Test
    fun `infinite values are refused`() {
        assertThrows<IllegalArgumentException> { AttributeCommands.setBase("minecraft:scale", Double.POSITIVE_INFINITY) }
    }
}
