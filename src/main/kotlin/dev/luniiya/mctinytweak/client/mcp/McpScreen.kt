package dev.luniiya.mctinytweak.client.mcp

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

    override fun init() {
        val wide = 310
        val half = (wide - 10) / 2
        val left = (width - wide) / 2
        var y = TOP + 20

        addRenderableWidget(
            CycleButton.onOffBuilder(Mcp.settings.enabled)
                .withTooltip { Tooltip.create(Component.translatable("screen.mc_tiny_tweak.mcp.enabled.tooltip")) }
                .create(left, y, wide, 20, Component.translatable("screen.mc_tiny_tweak.mcp.enabled")) { _, value ->
                    Mcp.update { it.copy(enabled = value) }
                    rebuildWidgets()
                },
        )
        y += 28
        for ((index, feature) in Feature.entries.withIndex()) {
            val x = if (index % 2 == 0) left else left + half + 10
            val key = "screen.mc_tiny_tweak.mcp.feature.${feature.key}"
            val button = addRenderableWidget(
                CycleButton.onOffBuilder(Mcp.settings.allows(feature))
                    .withTooltip { Tooltip.create(Component.translatable("$key.tooltip")) }
                    .create(x, y, half, 20, Component.translatable(key)) { _, value -> Mcp.update { it.with(feature, value) } },
            )
            button.active = Mcp.settings.enabled
            if (index % 2 == 1) y += 24
        }
        if (Feature.entries.size % 2 == 1) y += 24
        y += 22

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
        y += 28
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(left, y, wide, 20).build())
        statusY = y - 28 - 14
    }

    private var statusY = 0

    private fun status(): Component {
        val port = Mcp.port
        val error = Mcp.startError
        return when {
            !Mcp.settings.enabled -> Component.translatable("screen.mc_tiny_tweak.mcp.status.off").withStyle(ChatFormatting.GRAY)
            port != null -> Component.translatable("screen.mc_tiny_tweak.mcp.status.on", "127.0.0.1:$port").withStyle(ChatFormatting.GREEN)
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
        const val TOP = 20
    }
}
