package dev.luniiya.mctinytweak.client

import com.mojang.blaze3d.platform.InputConstants
import dev.luniiya.mctinytweak.defaults.PersonalDefaults

/** Replaces keybind defaults with the personal ones, so Reset in Controls restores them. */
object PersonalKeyDefaults {
    @JvmStatic
    fun defaultFor(name: String, original: InputConstants.Key): InputConstants.Key {
        val keyName = PersonalDefaults.keybinds[name] ?: return original
        return runCatching { InputConstants.getKey(keyName) }.getOrDefault(original)
    }
}
