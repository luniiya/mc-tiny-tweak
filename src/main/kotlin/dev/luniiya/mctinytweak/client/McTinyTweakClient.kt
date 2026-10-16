package dev.luniiya.mctinytweak.client

import com.mojang.blaze3d.platform.InputConstants
import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.client.freelook.Freelook
import dev.luniiya.mctinytweak.client.tweaks.Tweaks
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier

object McTinyTweakClient : ClientModInitializer {
    val category: KeyMapping.Category =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(McTinyTweak.MOD_ID, "main"))

    /** Opens the mod menu. Defaults to backslash and can be rebound in Controls. */
    val openMenuKey: KeyMapping = KeyMapping(
        "key.${McTinyTweak.MOD_ID}.open_menu",
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_BACKSLASH,
        category,
    )

    /** Hold to look around without turning. Defaults to left control. */
    val freelookKey: KeyMapping = Freelook.keyMapping(category)

    override fun onInitializeClient() {
        Tweaks.load()
        KeyBindingHelper.registerKeyBinding(openMenuKey)
        KeyBindingHelper.registerKeyBinding(freelookKey)
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            Freelook.tick(client, freelookKey)
            while (openMenuKey.consumeClick()) {
                if (client.screen == null) client.setScreen(ModMenuScreen(null))
            }
        }
    }
}
