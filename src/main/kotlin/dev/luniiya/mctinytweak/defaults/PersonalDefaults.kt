package dev.luniiya.mctinytweak.defaults

import java.nio.file.Files
import java.nio.file.Path

/**
 * The user's own options.txt, bundled in the mod as the game's defaults.
 * Edit `mc_tiny_tweak/default-options.txt` to change them permanently.
 */
object PersonalDefaults {
    const val RESOURCE = "/mc_tiny_tweak/default-options.txt"
    private const val KEY_PREFIX = "key_"

    val text: String by lazy {
        val stream = checkNotNull(PersonalDefaults::class.java.getResourceAsStream(RESOURCE)) { "Missing $RESOURCE" }
        stream.bufferedReader().use { it.readText() }
    }

    val options: Map<String, String> by lazy { parse(text) }

    /** Keybind name, such as `key.attack`, to key name, such as `key.keyboard.q`. */
    val keybinds: Map<String, String> by lazy { keybindsOf(options) }

    /** Parses options.txt lines of the form `name:value`, splitting at the first colon. */
    fun parse(text: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        for (line in text.lineSequence()) {
            val separator = line.indexOf(':')
            if (separator > 0) result[line.substring(0, separator)] = line.substring(separator + 1)
        }
        return result
    }

    fun keybindsOf(options: Map<String, String>): Map<String, String> =
        options.filterKeys { it.startsWith(KEY_PREFIX) }.mapKeys { it.key.removePrefix(KEY_PREFIX) }

    /** Writes the defaults when the game has no options file yet, so existing settings are never replaced. */
    fun writeIfMissing(optionsFile: Path, defaults: String = text): Boolean {
        if (Files.exists(optionsFile)) return false
        optionsFile.parent?.let(Files::createDirectories)
        Files.writeString(optionsFile, defaults)
        return true
    }
}
