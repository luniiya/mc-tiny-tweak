package dev.luniiya.mctinytweak.client

import com.mojang.blaze3d.platform.InputConstants
import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.client.baritone.BaritoneLog
import dev.luniiya.mctinytweak.client.freecam.AgentScreen
import dev.luniiya.mctinytweak.client.freecam.AgentStatusCard
import dev.luniiya.mctinytweak.client.freecam.Freecam
import dev.luniiya.mctinytweak.client.freecam.FreecamHud
import dev.luniiya.mctinytweak.client.freelook.Freelook
import dev.luniiya.mctinytweak.client.inventorypresets.InventoryPresetBar
import dev.luniiya.mctinytweak.client.inventorypresets.InventoryPresetStore
import dev.luniiya.mctinytweak.client.inventorypresets.StarterInventory
import dev.luniiya.mctinytweak.client.mcp.AgentInput
import dev.luniiya.mctinytweak.client.mcp.BaritoneBridge
import dev.luniiya.mctinytweak.client.mcp.BaritoneSettings
import dev.luniiya.mctinytweak.client.mcp.Mcp
import dev.luniiya.mctinytweak.client.mcp.PortalBuilder
import dev.luniiya.mctinytweak.client.tweaks.Tweaks
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
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

    /** Hold to look around without turning. Defaults to middle click (pick block moves to P). */
    val freelookKey: KeyMapping = Freelook.keyMapping(category)

    /** Turns freecam on and off. */
    val freecamKey: KeyMapping = Freecam.toggleKey(category)

    /** While an agent plays, swaps between the freecam and the agent's eyes. */
    val agentViewKey: KeyMapping = Freecam.agentViewKey(category)

    /** Turns fullbright on and off. */
    val fullbrightKey: KeyMapping = Tweaks.fullbrightKey(category)

    override fun onInitializeClient() {
        Tweaks.load()
        KeyBindingHelper.registerKeyBinding(openMenuKey)
        KeyBindingHelper.registerKeyBinding(freelookKey)
        KeyBindingHelper.registerKeyBinding(freecamKey)
        KeyBindingHelper.registerKeyBinding(agentViewKey)
        KeyBindingHelper.registerKeyBinding(fullbrightKey)
        AgentStatusCard.register()
        FreecamHud.register()
        InventoryPresetStore.register()
        InventoryPresetBar.register()
        Mcp.load()
        ClientLifecycleEvents.CLIENT_STARTED.register {
            if (BaritoneBridge.installed) {
                BaritoneSettings.applyNewModDefaults()
                BaritoneLog.install()
            }
        }
        // Before vanilla reads key states, so agent-held keys count for the whole tick.
        ClientTickEvents.START_CLIENT_TICK.register(AgentInput::tick)
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            PortalBuilder.tick(client)
            StarterInventory.tick(client)
            while (freecamKey.consumeClick()) Freecam.toggle()
            while (agentViewKey.consumeClick()) Freecam.toggleAgentView()
            while (fullbrightKey.consumeClick()) Tweaks.toggleFullbright()
            Freecam.tick(client)
            AgentScreen.tick(client)
            Freelook.tick(client, freelookKey)
            while (openMenuKey.consumeClick()) {
                if (client.screen == null) client.setScreen(ModMenuScreen(null))
            }
        }
    }
}
