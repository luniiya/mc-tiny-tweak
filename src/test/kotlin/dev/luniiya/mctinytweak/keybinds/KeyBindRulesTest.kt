package dev.luniiya.mctinytweak.keybinds

import dev.luniiya.mctinytweak.defaults.PersonalDefaults
import dev.luniiya.mctinytweak.keybinds.KeyBindRules.InputMode
import dev.luniiya.mctinytweak.mixin.client.keybinds.KeyBindsScreenAccessor
import dev.luniiya.mctinytweak.mixin.client.keybinds.KeyEntryAccessor
import net.minecraft.client.gui.screens.options.controls.KeyBindsList
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KeyBindRulesTest {
    @Test
    fun `name search matches bind name or category ignoring case`() {
        assertTrue(KeyBindRules.matchesName("", "Jump", "Movement"))
        assertTrue(KeyBindRules.matchesName("jum", "Jump", "Movement"))
        assertTrue(KeyBindRules.matchesName(" MOVE ", "Jump", "Movement"))
        assertFalse(KeyBindRules.matchesName("sneak", "Jump", "Movement"))
    }

    @Test
    fun `key search matches the exact bound key or everything when unset`() {
        assertTrue(KeyBindRules.matchesKey(null, "key.keyboard.q"))
        assertTrue(KeyBindRules.matchesKey("key.keyboard.q", "key.keyboard.q"))
        assertFalse(KeyBindRules.matchesKey("key.keyboard.q", "key.keyboard.e"))
        assertFalse(KeyBindRules.matchesKey("key.mouse.left", "key.keyboard.unknown"))
    }

    @Test
    fun `extras are the binds the personal defaults do not list`() {
        val defaults = mapOf("key.attack" to "key.keyboard.q", "key.voice" to "key.keyboard.unknown")
        assertEquals(listOf("key.other_mod.thing"), KeyBindRules.extras(listOf("key.attack", "key.other_mod.thing", "key.voice"), defaults))
    }

    @Test
    fun `input modes bind break and place and are detected from the current binds`() {
        assertEquals("key.keyboard.q" to "key.keyboard.e", InputMode.TABLET.attack to InputMode.TABLET.use)
        assertEquals("key.mouse.left" to "key.mouse.right", InputMode.MOUSE.attack to InputMode.MOUSE.use)
        assertEquals(InputMode.TABLET, InputMode.detect("key.keyboard.q", "key.keyboard.e"))
        assertEquals(InputMode.MOUSE, InputMode.detect("key.mouse.left", "key.mouse.right"))
        assertEquals(InputMode.MOUSE, InputMode.detect("key.keyboard.q", "key.mouse.right"))
    }

    @Test
    fun `key binds screen mixins apply to the vanilla classes`() {
        // Loading the targets runs the mixins, so a missing field or injection point fails here instead of in game.
        assertTrue(KeyBindsScreenAccessor::class.java.isAssignableFrom(KeyBindsScreen::class.java))
        assertTrue(KeyEntryAccessor::class.java.isAssignableFrom(KeyBindsList.KeyEntry::class.java))
    }

    @Test
    fun `tablet mode moves freelook to left click and mouse mode puts it back on left control`() {
        assertEquals("key.mouse.left", KeyBindRules.InputMode.TABLET.freelook)
        assertEquals("key.keyboard.left.control", KeyBindRules.InputMode.MOUSE.freelook)
        assertEquals(PersonalDefaults.keybinds["key.mc_tiny_tweak.freelook"], KeyBindRules.InputMode.MOUSE.freelook)
        for (mode in KeyBindRules.InputMode.entries) {
            assertNotEquals(mode.attack, mode.freelook, "freelook never shares a button with attack in $mode")
        }
    }
}
