package dev.luniiya.mctinytweak.client

import com.mojang.blaze3d.platform.InputConstants
import dev.luniiya.mctinytweak.McTinyTweak
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

    override fun onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(openMenuKey)
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (openMenuKey.consumeClick()) {
                if (client.screen == null) client.setScreen(ModMenuScreen(null))
            }
        }
    }
}
