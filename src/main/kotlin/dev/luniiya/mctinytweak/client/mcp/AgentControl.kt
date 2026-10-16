package dev.luniiya.mctinytweak.client.mcp

import com.mojang.blaze3d.platform.InputConstants
import dev.luniiya.mctinytweak.client.McTinyTweakClient
import dev.luniiya.mctinytweak.client.Notices
import dev.luniiya.mctinytweak.mixin.client.mcp.KeyMappingAccessor
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

/**
 * Who controls the player. While the agent has taken over, the keyboard and mouse no longer move or act,
 * except for the mod menu key, so the player can always open Mod menu > MCP and take control back.
 */
object AgentControl {
    @Volatile
    private var agent = false

    @JvmStatic
    fun agentInControl() = agent

    /** True when a real key event on [key] must be ignored. */
    @JvmStatic
    fun blocksKey(key: InputConstants.Key): Boolean =
        agent && key != (McTinyTweakClient.openMenuKey as KeyMappingAccessor).`mc_tiny_tweak$getKey`()

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
