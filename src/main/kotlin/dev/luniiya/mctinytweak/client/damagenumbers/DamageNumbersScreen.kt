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
        Toggle("combo_counter", { it.comboCounter }) { s, v -> s.copy(comboCounter = v) },
        Toggle("heal_numbers", { it.healNumbers }) { s, v -> s.copy(healNumbers = v) },
        Toggle("slam", { it.slam }) { s, v -> s.copy(slam = v) },
        Toggle("flash", { it.flash }) { s, v -> s.copy(flash = v) },
        Toggle("shake", { it.shake }) { s, v -> s.copy(shake = v) },
        Toggle("burst", { it.burst }) { s, v -> s.copy(burst = v) },
        Toggle("gradient", { it.gradient }) { s, v -> s.copy(gradient = v) },
        Toggle("combo", { it.combo }) { s, v -> s.copy(combo = v) },
        Toggle("count_up", { it.countUp }) { s, v -> s.copy(countUp = v) },
        Toggle("crit_mark", { it.critMark }) { s, v -> s.copy(critMark = v) },
        Toggle("font", { it.font }) { s, v -> s.copy(font = v) },
        Toggle("sounds", { it.sounds }) { s, v -> s.copy(sounds = v) },
        Toggle("rings", { it.rings }) { s, v -> s.copy(rings = v) },
        Toggle("slash", { it.slash }) { s, v -> s.copy(slash = v) },
        Toggle("afterimages", { it.afterimages }) { s, v -> s.copy(afterimages = v) },
        Toggle("rays", { it.rays }) { s, v -> s.copy(rays = v) },
        Toggle("shatter", { it.shatter }) { s, v -> s.copy(shatter = v) },
        Toggle("focus_lines", { it.focusLines }) { s, v -> s.copy(focusLines = v) },
        Toggle("rainbow", { it.rainbow }) { s, v -> s.copy(rainbow = v) },
        Toggle("impact_frame", { it.impactFrame }) { s, v -> s.copy(impactFrame = v) },
    )

    private var titleY = 0

    override fun init() {
        val buttonWidth = 150
        val left = width / 2 - buttonWidth - 5
        val right = width / 2 + 5
        val rows = (toggles.size + 1) / 2
        // The master row, the grid, a gap and Done, centered as a whole.
        val total = 24 + rows * ROW + 6 + 20
        titleY = maxOf(4, (height - total) / 2 - 12)
        var y = titleY + 12
        addRenderableWidget(
            CycleButton.onOffBuilder(Tweaks.damageNumbers())
                .withTooltip { Tooltip.create(Component.translatable("screen.mc_tiny_tweak.tweaks.damage_numbers.tooltip")) }
                .create(left, y, buttonWidth, 20, Component.translatable("screen.mc_tiny_tweak.tweaks.damage_numbers")) { _, value ->
                    Tweaks.update { it.copy(damageNumbers = value) }
                },
        )
        addRenderableWidget(
            CycleButton.onOffBuilder(DamageNumberPopups.style.onlyMine)
                .withTooltip { Tooltip.create(Component.translatable("screen.mc_tiny_tweak.damage_numbers.only_mine.tooltip")) }
                .create(right, y, buttonWidth, 20, Component.translatable("screen.mc_tiny_tweak.damage_numbers.only_mine")) { _, value ->
                    DamageNumberPopups.update { it.copy(onlyMine = value) }
                },
        )
        y += 24
        toggles.forEachIndexed { i, toggle ->
            val prefix = "screen.mc_tiny_tweak.damage_numbers.${toggle.key}"
            addRenderableWidget(
                CycleButton.onOffBuilder(toggle.get(DamageNumberPopups.style))
                    .withTooltip { Tooltip.create(Component.translatable("$prefix.tooltip")) }
                    .create(if (i % 2 == 0) left else right, y + i / 2 * ROW, buttonWidth, 20, Component.translatable(prefix)) { _, value ->
                        DamageNumberPopups.update { toggle.set(it, value) }
                    },
            )
        }
        y += rows * ROW + 6
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(width / 2 - 100, y, 200, 20).build())
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, titleY, 0xFFFFFFFF.toInt())
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen() = false

    private companion object {
        const val ROW = 21
    }
}
