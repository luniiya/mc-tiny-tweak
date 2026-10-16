package dev.luniiya.mctinytweak.client.attributes

import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.attributes.AttributePresets
import dev.luniiya.mctinytweak.client.HiddenCommandFeedback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.DefaultAttributes
import java.nio.file.Files
import java.nio.file.Path

/** Reads the local player's attributes and keeps the saved presets. */
object PlayerAttributes {
    private val file: Path get() = FabricLoader.getInstance().configDir.resolve(AttributePresets.FILE_NAME)

    private val builtIn by lazy { AttributePresets.bundled() }
    var presets = AttributePresets()
        private set
    private var loaded = false

    fun holders(player: LocalPlayer): List<Holder<Attribute>> =
        BuiltInRegistries.ATTRIBUTE.listElements().filter { player.attributes.hasAttribute(it) }.toList()

    fun default(holder: Holder<Attribute>) = DefaultAttributes.getSupplier(EntityType.PLAYER).getBaseValue(holder)

    fun baseValues(player: LocalPlayer) = holders(player).associate { it.registeredName to player.attributes.getBaseValue(it) }

    fun defaults(player: LocalPlayer) = holders(player).associate { it.registeredName to default(it) }

    /** Sends what the preset needs and mirrors it locally, so attributes the server does not sync still show it. */
    fun load(player: LocalPlayer, name: String, quiet: Boolean = false): Int {
        val commands = presets().loadCommands(name, baseValues(player), defaults(player))
        if (quiet) {
            val resets = commands.count { it.endsWith(" base reset") }
            HiddenCommandFeedback.hide(listOf("commands.attribute.base_value.set.success"), commands.size - resets)
            HiddenCommandFeedback.hide(listOf("commands.attribute.base_value.reset.success"), resets)
        }
        commands.forEach(player.connection::sendCommand)
        val target = presets().values(name).orEmpty()
        for (holder in holders(player)) {
            val instance = player.attributes.getInstance(holder) ?: continue
            instance.baseValue = target[holder.registeredName] ?: default(holder)
        }
        return commands.size
    }

    fun presets(): AttributePresets {
        if (!loaded) {
            loaded = true
            presets = try {
                if (Files.exists(file)) AttributePresets.parse(Files.readString(file), builtIn) else AttributePresets(builtIn = builtIn)
            } catch (e: Exception) {
                McTinyTweak.logger.warn("Could not read {}, starting with no presets", file, e)
                AttributePresets(builtIn = builtIn)
            }
        }
        return presets
    }

    fun update(change: (AttributePresets) -> AttributePresets) {
        presets = change(presets())
        try {
            Files.createDirectories(file.parent)
            Files.writeString(file, presets.serialize())
        } catch (e: Exception) {
            McTinyTweak.logger.warn("Could not save {}", file, e)
        }
    }
}
