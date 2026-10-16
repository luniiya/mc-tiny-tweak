package dev.luniiya.mctinytweak.client.op

import dev.luniiya.mctinytweak.client.attributes.AttributeEditorScreen
import dev.luniiya.mctinytweak.client.itemeditor.ItemEditorScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Tools that need creative mode or operator commands. */
class OpScreen(private val parent: Screen?) : Screen(Component.translatable("screen.mc_tiny_tweak.op")) {
    override fun init() {
        val buttonWidth = 160
        val x = (width - buttonWidth) / 2
        var y = height / 2 - 32
        addRenderableWidget(
            Button.builder(Component.translatable("screen.mc_tiny_tweak.item_editor")) {
                minecraft.setScreen(ItemEditorScreen.forHeldItem(minecraft, this))
            }.bounds(x, y, buttonWidth, 20).build(),
        )
        y += 24
        addRenderableWidget(
            Button.builder(Component.translatable("screen.mc_tiny_tweak.attributes")) {
                minecraft.setScreen(AttributeEditorScreen(this))
            }.bounds(x, y, buttonWidth, 20).build(),
        )
        y += 32
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(x, y, buttonWidth, 20).build())
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 60, 0xFFFFFFFF.toInt())
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen() = false
}
