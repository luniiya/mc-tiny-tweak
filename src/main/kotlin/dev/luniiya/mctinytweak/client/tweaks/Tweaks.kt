package dev.luniiya.mctinytweak.client.tweaks

import dev.luniiya.mctinytweak.McTinyTweak
import com.mojang.blaze3d.platform.InputConstants
import dev.luniiya.mctinytweak.client.Notices
import dev.luniiya.mctinytweak.settings.TweakSettings
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.KeyMapping
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.nio.file.Files
import java.nio.file.Path

/** The current tweak settings, loaded from and saved to the config folder. */
object Tweaks {
    private val file: Path get() = FabricLoader.getInstance().configDir.resolve(TweakSettings.FILE_NAME)

    var settings = TweakSettings()
        private set

    @JvmStatic
    fun showArmor() = settings.showArmor

    @JvmStatic
    fun fullbright() = settings.fullbright

    @JvmStatic
    fun compactHealth() = settings.compactHealth

    @JvmStatic
    fun damageNumbers() = settings.damageNumbers

    /** Defaults to G, like the fullbright mod the user had before. */
    fun fullbrightKey(category: KeyMapping.Category) =
        KeyMapping("key.${McTinyTweak.MOD_ID}.fullbright", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, category)

    fun toggleFullbright() {
        update { it.copy(fullbright = !it.fullbright) }
        Notices.show(Component.translatable(if (fullbright()) "notice.mc_tiny_tweak.fullbright.on" else "notice.mc_tiny_tweak.fullbright.off"))
    }

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
