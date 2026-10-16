package dev.luniiya.mctinytweak.client.tweaks

import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.settings.TweakSettings
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

/** The current tweak settings, loaded from and saved to the config folder. */
object Tweaks {
    private val file: Path get() = FabricLoader.getInstance().configDir.resolve(TweakSettings.FILE_NAME)

    var settings = TweakSettings()
        private set

    @JvmStatic
    fun showArmor() = settings.showArmor

    fun load() {
        settings = try {
            if (Files.exists(file)) TweakSettings.parse(Files.readString(file)) else TweakSettings()
        } catch (e: Exception) {
            McTinyTweak.logger.warn("Could not read {}, using defaults", file, e)
            TweakSettings()
        }
    }

    fun update(change: (TweakSettings) -> TweakSettings) {
        settings = change(settings)
        try {
            Files.createDirectories(file.parent)
            Files.writeString(file, settings.serialize())
        } catch (e: Exception) {
            McTinyTweak.logger.warn("Could not save {}", file, e)
        }
    }
}
