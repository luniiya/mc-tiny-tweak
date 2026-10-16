package dev.luniiya.mctinytweak.client.mcp

import baritone.api.BaritoneAPI
import baritone.api.Settings
import baritone.api.utils.SettingsUtil
import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.baritone.BaritoneDefaults
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files

/**
 * Reads and changes Baritone's settings, with the mod's own defaults on top of Baritone's. Like
 * [BaritoneBridge], only call this when Baritone is installed. Client thread only.
 */
object BaritoneSettings {
    /** One setting as text. [modified] compares against the mod's default, not Baritone's. */
    data class Entry(val name: String, val type: String, val value: String, val default: String, val isBoolean: Boolean) {
        val modified get() = value != default
    }

    private val modDefaults by lazy { BaritoneDefaults.load() }

    private val settings: Settings get() = BaritoneAPI.getSettings()

    private fun editable() = settings.allSettings.filter { !it.isJavaOnly }

    fun entries(): List<Entry> = editable().map(::entry)

    fun entry(name: String): Entry? = settings.byLowerName[name.lowercase()]?.takeIf { !it.isJavaOnly }?.let(::entry)

    private fun entry(setting: Settings.Setting<*>) = Entry(
        name = setting.name,
        type = SettingsUtil.settingTypeToString(setting),
        value = SettingsUtil.settingValueToString(setting),
        default = BaritoneDefaults.defaultFor(setting.name, SettingsUtil.settingDefaultToString(setting), modDefaults),
        isBoolean = setting.valueClass == java.lang.Boolean::class.java,
    )

    /** Sets one value from text, as Baritone's `#set` would. Throws [IllegalArgumentException] when it does not parse. */
    fun set(name: String, value: String, save: Boolean = true) {
        val setting = settings.byLowerName[name.lowercase()]
        require(setting != null && !setting.isJavaOnly) { "Unknown Baritone setting: $name" }
        require(setting.valueClass != java.lang.Boolean::class.java || BaritoneDefaults.isBooleanText(value)) {
            "${setting.name} is on or off: use true or false"
        }
        try {
            SettingsUtil.parseAndApply(settings, setting.name.lowercase(), value)
        } catch (e: Exception) {
            throw IllegalArgumentException("$value is not a valid ${SettingsUtil.settingTypeToString(setting)} for ${setting.name}")
        }
        if (save) save()
    }

    fun save() = SettingsUtil.save(settings)

    /** Puts every setting back to the mod's default, or Baritone's where the mod has none. */
    fun resetToModDefaults() {
        for (setting in editable()) setting.reset()
        for ((name, value) in modDefaults) {
            try {
                set(name, value, save = false)
            } catch (e: IllegalArgumentException) {
                McTinyTweak.logger.warn("Skipping Baritone default {}: {}", name, e.message)
            }
        }
        save()
    }

    /** Gives a new instance the mod's defaults once; after that the player's own changes are kept. */
    fun applyModDefaultsOnce() {
        val marker = FabricLoader.getInstance().configDir.resolve(BaritoneDefaults.APPLIED_MARKER)
        if (Files.exists(marker)) return
        for ((name, value) in modDefaults) {
            try {
                set(name, value, save = false)
            } catch (e: IllegalArgumentException) {
                McTinyTweak.logger.warn("Skipping Baritone default {}: {}", name, e.message)
            }
        }
        save()
        try {
            Files.createDirectories(marker.parent)
            Files.writeString(marker, "Baritone defaults from MC Tiny Tweak were applied. Delete to apply them again.\n")
        } catch (e: Exception) {
            McTinyTweak.logger.warn("Could not write {}", marker, e)
        }
    }
}
