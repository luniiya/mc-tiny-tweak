package dev.luniiya.mctinytweak.client.freecam

import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.client.McTinyTweakClient
import dev.luniiya.mctinytweak.client.mcp.AgentControl
import dev.luniiya.mctinytweak.client.mcp.BaritoneBridge
import dev.luniiya.mctinytweak.notify.NoticeStyle
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.ChatFormatting
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier

/** A small tooltip-style card in the top right corner that says what the agent is doing while it plays. */
object AgentStatusCard {
    /** The last tool the agent called, as shown in chat. */
    @Volatile
    var lastAction: String? = null

    private const val MAX_WIDTH = 180
    private const val MARGIN = 6
    private const val LINE = 10

    fun register() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.STATUS_EFFECTS,
            Identifier.fromNamespaceAndPath(McTinyTweak.MOD_ID, "agent_status"),
            ::render,
        )
    }

    private fun render(graphics: GuiGraphics, delta: DeltaTracker) {
        if (!AgentControl.agentInControl() || Freecam.screenshotView) return
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        val font = minecraft.font
        val lines = buildList {
            add(Component.translatable("hud.mc_tiny_tweak.agent.title").withStyle(Style.EMPTY.withColor(NoticeStyle.TAG_COLOR)))
            lastAction?.let { add(Component.translatable("hud.mc_tiny_tweak.agent.last", it).withStyle(ChatFormatting.GRAY)) }
            if (BaritoneBridge.installed) BaritoneBridge.activeProcess()?.let {
                add(Component.translatable("hud.mc_tiny_tweak.agent.baritone", it).withStyle(ChatFormatting.GRAY))
            }
            add(
                Component.translatable(
                    "hud.mc_tiny_tweak.agent.vitals",
                    "%.0f".format(player.health),
                    player.foodData.foodLevel,
                ).withStyle(ChatFormatting.GRAY),
            )
            val key = McTinyTweakClient.agentViewKey.translatedKeyMessage
            add(
                Component.translatable(
                    if (Freecam.watchingAgent) "hud.mc_tiny_tweak.agent.to_freecam" else "hud.mc_tiny_tweak.agent.to_agent",
                    key,
                ).withStyle(ChatFormatting.DARK_GRAY),
            )
        }
        val texts = lines.map { font.substrByWidth(it, MAX_WIDTH) }
        val width = texts.maxOf { font.width(it) }
        val height = texts.size * LINE - 2
        val x = graphics.guiWidth() - width - MARGIN - TooltipRenderUtil.PADDING_RIGHT
        val y = MARGIN + TooltipRenderUtil.PADDING_TOP + effectRows(player) * 25
        TooltipRenderUtil.renderTooltipBackground(graphics, x, y, width, height, null)
        for ((index, text) in texts.withIndex()) {
            graphics.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(text), x, y + index * LINE, -1, true)
        }
    }

    /** Vanilla draws effect icons in up to two rows at the top right; the card goes below them. */
    private fun effectRows(player: net.minecraft.world.entity.player.Player): Int {
        val visible = player.activeEffects.filter { it.showIcon() }
        return listOf(visible.any { it.effect.value().isBeneficial }, visible.any { !it.effect.value().isBeneficial }).count { it }
    }
}
