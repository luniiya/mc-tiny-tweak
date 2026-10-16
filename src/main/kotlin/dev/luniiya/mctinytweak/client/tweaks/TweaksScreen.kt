package dev.luniiya.mctinytweak.client.tweaks

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Small toggles that change how the game looks. */
class TweaksScreen(private val parent: Screen?) : Screen(Component.translatable("screen.mc_tiny_tweak.tweaks")) {
    override fun init() {
        val buttonWidth = 200
        val x = (width - buttonWidth) / 2
        var y = height / 2 - 20
        addRenderableWidget(
            CycleButton.onOffBuilder(Tweaks.settings.showArmor)
                .withTooltip { Tooltip.create(Component.translatable("screen.mc_tiny_tweak.tweaks.show_armor.tooltip")) }
                .create(x, y, buttonWidth, 20, Component.translatable("screen.mc_tiny_tweak.tweaks.show_armor")) { _, value ->
                    Tweaks.update { it.copy(showArmor = value) }
                },
        )
        y += 32
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(x, y, buttonWidth, 20).build())
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 48, 0xFFFFFFFF.toInt())
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen() = false
}
