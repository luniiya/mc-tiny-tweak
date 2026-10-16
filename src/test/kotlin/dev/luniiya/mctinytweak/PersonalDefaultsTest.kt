package dev.luniiya.mctinytweak

import com.mojang.blaze3d.platform.InputConstants
import dev.luniiya.mctinytweak.defaults.PersonalDefaults
import net.minecraft.client.KeyMapping
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.lwjgl.glfw.GLFW
import java.nio.file.Files
import java.nio.file.Path

class PersonalDefaultsTest {
    @Test
    fun `parse splits at the first colon and skips malformed lines`() {
        val parsed = PersonalDefaults.parse("fov:0.875\nresourcePacks:[\"vanilla\"]\nkey_key.attack:key.keyboard.q\nbroken\n:x\n")
        assertEquals(mapOf("fov" to "0.875", "resourcePacks" to "[\"vanilla\"]", "key_key.attack" to "key.keyboard.q"), parsed)
        assertEquals(mapOf("key.attack" to "key.keyboard.q"), PersonalDefaults.keybindsOf(parsed))
    }

    @Test
    fun `defaults are written only when no options file exists`(@TempDir dir: Path) {
        val file = dir.resolve("game/options.txt")
        assertTrue(PersonalDefaults.writeIfMissing(file, "fov:0.875\n"))
        assertEquals("fov:0.875\n", Files.readString(file))
        Files.writeString(file, "fov:0.0\n")
        assertFalse(PersonalDefaults.writeIfMissing(file, "fov:0.875\n"))
        assertEquals("fov:0.0\n", Files.readString(file))
    }

    @Test
    fun `bundled defaults hold the requested settings`() {
        val options = PersonalDefaults.options
        assertEquals(105, (options.getValue("fov").toDouble() * 40 + 70).toInt())
        assertEquals("false", options["highContrast"])
        assertFalse(options.getValue("resourcePacks").contains("high_contrast"))
        assertEquals("key.keyboard.q", PersonalDefaults.keybinds["key.attack"])
        for (machineSpecific in listOf("lastServer", "soundDevice", "startedCleanly")) {
            assertFalse(options.containsKey(machineSpecific), machineSpecific)
        }
    }

    @Test
    fun `bundled defaults bind the mod menu to backslash`() {
        assertEquals("key.keyboard.backslash", PersonalDefaults.keybinds["key.mc_tiny_tweak.open_menu"])
        assertEquals(
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_BACKSLASH).name,
            PersonalDefaults.keybinds["key.mc_tiny_tweak.open_menu"],
        )
    }

    @Test
    fun `sprint moves to Z and freelook takes middle click with pick block on P`() {
        assertEquals("key.keyboard.z", PersonalDefaults.keybinds["key.sprint"])
        assertEquals("key.mouse.middle", PersonalDefaults.keybinds["key.mc_tiny_tweak.freelook"])
        assertEquals("key.keyboard.p", PersonalDefaults.keybinds["key.pickItem"])
        val onP = PersonalDefaults.keybinds.filterValues { it == "key.keyboard.p" }.keys.filterNot { it.startsWith("key.debug.") }.toSet()
        assertEquals(setOf("key.pickItem"), onP, "pick block is the only bind on P")
        val sprint = KeyMapping("key.sprint", InputConstants.KEY_LCONTROL, KeyMapping.Category.MOVEMENT)
        assertEquals(InputConstants.getKey("key.keyboard.z"), sprint.defaultKey)
    }

    @Test
    fun `creative hotbar save and load activators are unbound so X and C only pick hotbar slots`() {
        // X and C are hotbar slots; with the load activator on X, pressing it in creative wiped the hotbar.
        assertEquals("key.keyboard.x", PersonalDefaults.keybinds["key.hotbar.1"])
        assertEquals("key.keyboard.unknown", PersonalDefaults.keybinds["key.loadToolbarActivator"])
        assertEquals("key.keyboard.unknown", PersonalDefaults.keybinds["key.saveToolbarActivator"])
    }

    @Test
    fun `key mappings take their default from the personal defaults`() {
        val listed = KeyMapping("key.attack", InputConstants.Type.MOUSE, 0, KeyMapping.Category.MISC)
        assertEquals(InputConstants.getKey("key.keyboard.q"), listed.defaultKey)
        assertTrue(listed.isDefault)

        val unlisted = KeyMapping("key.mc_tiny_tweak.test_unlisted", InputConstants.Type.KEYSYM, InputConstants.KEY_X, KeyMapping.Category.MISC)
        assertEquals(InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_X), unlisted.defaultKey)
    }
}
