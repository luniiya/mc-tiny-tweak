package dev.luniiya.mctinytweak.client.tweaks

import dev.luniiya.mctinytweak.client.McTinyTweakClient
import dev.luniiya.mctinytweak.client.freecam.Freecam
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Small toggles that change how the game looks or plays. */
class TweaksScreen(private val parent: Screen?) : Screen(Component.translatable("screen.mc_tiny_tweak.tweaks")) {
    private var fullbrightButton: CycleButton<Boolean>? = null
    private var freecamButton: CycleButton<Boolean>? = null

    override fun init() {
        val buttonWidth = 200
        val x = (width - buttonWidth) / 2
        var y = height / 2 - 44
        addRenderableWidget(
            CycleButton.onOffBuilder(Tweaks.settings.showArmor)
                .withTooltip { Tooltip.create(Component.translatable("screen.mc_tiny_tweak.tweaks.show_armor.tooltip")) }
                .create(x, y, buttonWidth, 20, Component.translatable("screen.mc_tiny_tweak.tweaks.show_armor")) { _, value ->
                    Tweaks.update { it.copy(showArmor = value) }
                },
        )
        y += 24
        addRenderableWidget(
            CycleButton.onOffBuilder(Tweaks.settings.compactHealth)
                .withTooltip { Tooltip.create(Component.translatable("screen.mc_tiny_tweak.tweaks.compact_health.tooltip")) }
                .create(x, y, buttonWidth, 20, Component.translatable("screen.mc_tiny_tweak.tweaks.compact_health")) { _, value ->
                    Tweaks.update { it.copy(compactHealth = value) }
                },
        )
        y += 24
        fullbrightButton = addRenderableWidget(
            CycleButton.onOffBuilder(Tweaks.settings.fullbright)
                .withTooltip {
                    Tooltip.create(
                        Component.translatable(
                            "screen.mc_tiny_tweak.tweaks.fullbright.tooltip",
                            McTinyTweakClient.fullbrightKey.translatedKeyMessage,
                        ),
                    )
                }
                .create(x, y, buttonWidth, 20, Component.translatable("screen.mc_tiny_tweak.tweaks.fullbright")) { _, value ->
                    Tweaks.update { it.copy(fullbright = value) }
                },
        )
        y += 24
        freecamButton = addRenderableWidget(
            CycleButton.onOffBuilder(Freecam.toggled)
                .withTooltip { Tooltip.create(Component.translatable("screen.mc_tiny_tweak.freecam.tooltip", McTinyTweakClient.freecamKey.translatedKeyMessage)) }
                .create(x, y, buttonWidth, 20, Component.translatable("screen.mc_tiny_tweak.freecam")) { _, value ->
                    if (value != Freecam.toggled) Freecam.toggle()
                },
        ).also { it.active = minecraft.player != null }
        y += 32
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(x, y, buttonWidth, 20).build())
    }

    override fun tick() {
        // The key works while this screen is closed, so catch up if it changed meanwhile.
        fullbrightButton?.takeIf { it.value != Tweaks.fullbright() }?.value = Tweaks.fullbright()
        freecamButton?.takeIf { it.value != Freecam.toggled }?.value = Freecam.toggled
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
