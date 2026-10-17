package dev.luniiya.mctinytweak.client.damagenumbers

import dev.luniiya.mctinytweak.client.tweaks.Tweaks
import dev.luniiya.mctinytweak.damagenumbers.DamageNumberStyle
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Damage numbers on or off, and each of their effects. */
class DamageNumbersScreen(private val parent: Screen?) : Screen(Component.translatable("screen.mc_tiny_tweak.damage_numbers")) {
    private class Toggle(val key: String, val get: (DamageNumberStyle) -> Boolean, val set: (DamageNumberStyle, Boolean) -> DamageNumberStyle)

    private val toggles = listOf(
        Toggle("slam", { it.slam }) { s, v -> s.copy(slam = v) },
        Toggle("flash", { it.flash }) { s, v -> s.copy(flash = v) },
        Toggle("shake", { it.shake }) { s, v -> s.copy(shake = v) },
        Toggle("burst", { it.burst }) { s, v -> s.copy(burst = v) },
        Toggle("gradient", { it.gradient }) { s, v -> s.copy(gradient = v) },
        Toggle("combo", { it.combo }) { s, v -> s.copy(combo = v) },
        Toggle("count_up", { it.countUp }) { s, v -> s.copy(countUp = v) },
        Toggle("crit_mark", { it.critMark }) { s, v -> s.copy(critMark = v) },
    )

    override fun init() {
        val buttonWidth = 150
        val left = width / 2 - buttonWidth - 5
        val right = width / 2 + 5
        var y = height / 2 - 72
        addRenderableWidget(
            CycleButton.onOffBuilder(Tweaks.damageNumbers())
                .withTooltip { Tooltip.create(Component.translatable("screen.mc_tiny_tweak.tweaks.damage_numbers.tooltip")) }
                .create(width / 2 - 100, y, 200, 20, Component.translatable("screen.mc_tiny_tweak.tweaks.damage_numbers")) { _, value ->
                    Tweaks.update { it.copy(damageNumbers = value) }
                },
        )
        y += 28
        toggles.forEachIndexed { i, toggle ->
            val prefix = "screen.mc_tiny_tweak.damage_numbers.${toggle.key}"
            addRenderableWidget(
                CycleButton.onOffBuilder(toggle.get(DamageNumberPopups.style))
                    .withTooltip { Tooltip.create(Component.translatable("$prefix.tooltip")) }
                    .create(if (i % 2 == 0) left else right, y + i / 2 * 24, buttonWidth, 20, Component.translatable(prefix)) { _, value ->
                        DamageNumberPopups.update { toggle.set(it, value) }
                    },
            )
        }
        y += (toggles.size + 1) / 2 * 24 + 8
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(width / 2 - 100, y, 200, 20).build())
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 90, 0xFFFFFFFF.toInt())
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen() = false
}
