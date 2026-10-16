package dev.luniiya.mctinytweak.client.mcp

import dev.luniiya.mctinytweak.client.baritone.BaritoneLogScreen
import dev.luniiya.mctinytweak.client.baritone.BaritoneSettingsScreen
import dev.luniiya.mctinytweak.mcp.McpSettings
import dev.luniiya.mctinytweak.mcp.McpSettings.Feature
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Switches for the MCP server and for each kind of access an agent gets, saved for this instance. */
class McpScreen(private val parent: Screen?) : Screen(Component.translatable("screen.mc_tiny_tweak.mcp")) {
    private var copied = false
    private var shownControl = AgentControl.agentInControl()

    override fun init() {
        val wide = 310
        val half = (wide - 10) / 2
        val left = (width - wide) / 2
        var y = TOP + 16

        addRenderableWidget(
            CycleButton.onOffBuilder(Mcp.settings.enabled)
                .withTooltip { Tooltip.create(Component.translatable("screen.mc_tiny_tweak.mcp.enabled.tooltip")) }
                .create(left, y, wide, 20, Component.translatable("screen.mc_tiny_tweak.mcp.enabled")) { _, value ->
                    Mcp.update { it.copy(enabled = value) }
                    rebuildWidgets()
                },
        )
        y += ROW
        val control = addRenderableWidget(
            CycleButton.builder<Boolean>({ Component.translatable(if (it) "screen.mc_tiny_tweak.mcp.control.agent" else "screen.mc_tiny_tweak.mcp.control.player") }, AgentControl.agentInControl())
                .withValues(listOf(false, true))
                .withTooltip { Tooltip.create(Component.translatable("screen.mc_tiny_tweak.mcp.control.tooltip")) }
                .create(left, y, half, 20, Component.translatable("screen.mc_tiny_tweak.mcp.control")) { _, agent ->
                    if (agent) AgentControl.takeOver() else AgentControl.release(byPlayer = true)
                    shownControl = agent
                },
        )
        control.active = Mcp.settings.enabled && Mcp.settings.allows(Feature.TAKE_OVER)
        val actions = addRenderableWidget(
            CycleButton.onOffBuilder(Mcp.settings.showActions)
                .withTooltip { Tooltip.create(Component.translatable("screen.mc_tiny_tweak.mcp.show_actions.tooltip")) }
                .create(left + half + 10, y, half, 20, Component.translatable("screen.mc_tiny_tweak.mcp.show_actions")) { _, value ->
                    Mcp.update { it.copy(showActions = value) }
                },
        )
        actions.active = Mcp.settings.enabled
        y += ROW
        addRenderableWidget(
            CycleButton.onOffBuilder(Mcp.settings.wakeOnChat)
                .withTooltip { Tooltip.create(Component.translatable("screen.mc_tiny_tweak.mcp.chat_wake.tooltip")) }
                .create(left, y, wide, 20, Component.translatable("screen.mc_tiny_tweak.mcp.chat_wake")) { _, value ->
                    Mcp.update { it.copy(wakeOnChat = value) }
                },
        )
        y += ROW + 6
        for ((index, feature) in Feature.entries.withIndex()) {
            val x = if (index % 2 == 0) left else left + half + 10
            val key = "screen.mc_tiny_tweak.mcp.feature.${feature.key}"
            val button = addRenderableWidget(
                CycleButton.onOffBuilder(Mcp.settings.allows(feature))
                    .withTooltip { Tooltip.create(Component.translatable("$key.tooltip")) }
                    .create(x, y, half, 20, Component.translatable(key)) { _, value ->
                        Mcp.update { it.with(feature, value) }
                        if (feature == Feature.TAKE_OVER) rebuildWidgets()
                    },
            )
            button.active = Mcp.settings.enabled
            if (index % 2 == 1) y += ROW
        }
        if (Feature.entries.size % 2 == 1) y += ROW
        y += 4
        val baritone = addRenderableWidget(
            Button.builder(Component.translatable("screen.mc_tiny_tweak.baritone")) { minecraft.setScreen(BaritoneSettingsScreen(this)) }
                .bounds(left, y, half, 20)
                .tooltip(Tooltip.create(Component.translatable(if (BaritoneBridge.installed) "screen.mc_tiny_tweak.baritone.tooltip" else "screen.mc_tiny_tweak.baritone.missing")))
                .build(),
        )
        baritone.active = BaritoneBridge.installed
        val baritoneLog = addRenderableWidget(
            Button.builder(Component.translatable("screen.mc_tiny_tweak.baritone_log")) { minecraft.setScreen(BaritoneLogScreen(this)) }
                .bounds(left + half + 10, y, half, 20)
                .tooltip(Tooltip.create(Component.translatable(if (BaritoneBridge.installed) "screen.mc_tiny_tweak.baritone_log.tooltip" else "screen.mc_tiny_tweak.baritone.missing")))
                .build(),
        )
        baritoneLog.active = BaritoneBridge.installed
        y += ROW + 16

        val copy = addRenderableWidget(
            Button.builder(Component.translatable(if (copied) "screen.mc_tiny_tweak.mcp.copied" else "screen.mc_tiny_tweak.mcp.copy")) {
                Mcp.setupCommand()?.let { minecraft.keyboardHandler.clipboard = it }
                copied = true
                rebuildWidgets()
            }.bounds(left, y, half, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.mc_tiny_tweak.mcp.copy.tooltip")))
                .build(),
        )
        copy.active = Mcp.port != null
        addRenderableWidget(
            Button.builder(Component.translatable("screen.mc_tiny_tweak.mcp.new_token")) { confirmNewToken() }
                .bounds(left + half + 10, y, half, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.mc_tiny_tweak.mcp.new_token.tooltip")))
                .build(),
        )
        y += ROW + 2
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(left, y, wide, 20).build())
        statusY = y - ROW - 2 - 13
    }

    private var statusY = 0

    private fun status(): Component {
        val port = Mcp.port
        val error = Mcp.startError
        return when {
            !Mcp.settings.enabled -> Component.translatable("screen.mc_tiny_tweak.mcp.status.off").withStyle(ChatFormatting.GRAY)
            port != null -> Component.translatable("screen.mc_tiny_tweak.mcp.status.on", "127.0.0.1:$port", McpSettings.serverName(port)).withStyle(ChatFormatting.GREEN)
            else -> Component.translatable("screen.mc_tiny_tweak.mcp.status.failed", error ?: "?").withStyle(ChatFormatting.RED)
        }
    }

    private fun confirmNewToken() {
        minecraft.setScreen(
            ConfirmScreen(
                { confirmed ->
                    if (confirmed) {
                        Mcp.regenerateToken()
                        copied = false
                    }
                    minecraft.setScreen(this)
                },
                Component.translatable("screen.mc_tiny_tweak.mcp.new_token"),
                Component.translatable("screen.mc_tiny_tweak.mcp.new_token.confirm"),
            ),
        )
    }

    /** The agent can take over or let go while this screen is open. */
    override fun tick() {
        if (shownControl != AgentControl.agentInControl()) {
            shownControl = AgentControl.agentInControl()
            rebuildWidgets()
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, TOP, 0xFFFFFFFF.toInt())
        graphics.drawCenteredString(font, status(), width / 2, statusY, 0xFFFFFFFF.toInt())
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen() = false

    private companion object {
        const val TOP = 12
        const val ROW = 22
    }
}
