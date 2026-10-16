package dev.luniiya.mctinytweak.client

import dev.luniiya.mctinytweak.client.itemeditor.ItemEditorScreen
import dev.luniiya.mctinytweak.client.mcp.McpScreen
import dev.luniiya.mctinytweak.client.tweaks.TweaksScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component

/** The mod's hub, opened with the menu key. Each feature adds a button here. */
class ModMenuScreen(private val parent: Screen?) : Screen(Component.translatable("screen.mc_tiny_tweak.menu")) {
    override fun init() {
        val buttonWidth = 160
        val x = (width - buttonWidth) / 2
        var y = height / 2 - 20
        addRenderableWidget(
            Button.builder(Component.translatable("screen.mc_tiny_tweak.item_editor")) {
                minecraft.setScreen(ItemEditorScreen.forHeldItem(minecraft, this))
            }.bounds(x, y, buttonWidth, 20).build(),
        )
        y += 24
        addRenderableWidget(
            Button.builder(Component.translatable("screen.mc_tiny_tweak.tweaks")) {
                minecraft.setScreen(TweaksScreen(this))
            }.bounds(x, y, buttonWidth, 20).build(),
        )
        y += 24
        addRenderableWidget(
            Button.builder(Component.translatable("screen.mc_tiny_tweak.mcp")) {
                minecraft.setScreen(McpScreen(this))
            }.bounds(x, y, buttonWidth, 20).build(),
        )
        y += 32
        addRenderableWidget(Button.builder(Component.translatable("gui.done")) { onClose() }.bounds(x, y, buttonWidth, 20).build())
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 48, 0xFFFFFFFF.toInt())
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (McTinyTweakClient.openMenuKey.matches(event)) {
            onClose()
            return true
        }
        return super.keyPressed(event)
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen() = false
}
