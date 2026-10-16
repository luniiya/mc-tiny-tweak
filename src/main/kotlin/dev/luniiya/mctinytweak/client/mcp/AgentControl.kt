package dev.luniiya.mctinytweak.client.mcp

import com.mojang.blaze3d.platform.InputConstants
import dev.luniiya.mctinytweak.client.McTinyTweakClient
import dev.luniiya.mctinytweak.client.Notices
import dev.luniiya.mctinytweak.client.freecam.Freecam
import dev.luniiya.mctinytweak.mixin.client.mcp.KeyMappingAccessor
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

/**
 * Who controls the player. While the agent has taken over, the keyboard and mouse no longer move or act,
 * except for the mod menu, freecam, agent view, fullbright and chat keys, so the player can always talk, watch and
 * open Mod menu > MCP to take control back.
 */
object AgentControl {
    @Volatile
    private var agent = false

    @JvmStatic
    fun agentInControl() = agent

    /** True when a real key event on [key] must be ignored. */
    @JvmStatic
    fun blocksKey(key: InputConstants.Key): Boolean = when {
        agent -> exemptKeys().none { boundKey(it) == key }
        Freecam.holdsPlayerInput() -> Freecam.heldMappings(Minecraft.getInstance()).any { boundKey(it) == key }
        else -> false
    }

    /** Keys that keep working while the agent plays: the mod menu, freecam, agent view, fullbright and chat. */
    private fun exemptKeys() = listOf(
        McTinyTweakClient.openMenuKey,
        McTinyTweakClient.freecamKey,
        McTinyTweakClient.agentViewKey,
        McTinyTweakClient.fullbrightKey,
    ) + CHAT_KEYS.mapNotNull { KeyMapping.get(it) }

    /** Vanilla's chat and command keys, looked up by name. */
    private val CHAT_KEYS = listOf("key.chat", "key.command")

    private fun boundKey(mapping: KeyMapping) = (mapping as KeyMappingAccessor).`mc_tiny_tweak$getKey`()

    /** Client thread only. */
    fun takeOver() {
        if (agent) return
        agent = true
        AgentInput.touch()
        KeyMapping.releaseAll()
        Notices.show(Component.translatable("notice.mc_tiny_tweak.mcp.took_over"))
    }

    /** Client thread only. Stops everything the agent left running. */
    fun release(byPlayer: Boolean) {
        if (!agent) return
        agent = false
        AgentInput.releaseAll()
        if (BaritoneBridge.installed && Minecraft.getInstance().player != null) BaritoneBridge.stop()
        KeyMapping.setAll()
        Notices.show(Component.translatable(if (byPlayer) "notice.mc_tiny_tweak.mcp.player_took_back" else "notice.mc_tiny_tweak.mcp.released"))
    }
}
