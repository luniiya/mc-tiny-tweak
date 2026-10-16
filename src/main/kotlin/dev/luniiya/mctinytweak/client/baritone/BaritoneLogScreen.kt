package dev.luniiya.mctinytweak.client.baritone

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Everything Baritone said, newest at the bottom. Follows new lines while scrolled to the end. */
class BaritoneLogScreen(private val parent: Screen?) : Screen(Component.translatable("screen.mc_tiny_tweak.baritone_log")) {
    private var rows: List<FormattedCharSequence> = emptyList()
    private var shownVersion = -1L
    private var scroll = 0.0
    private var following = true

    private val listTop get() = 28
    private val listBottom get() = height - 36
    private val rowWidth get() = minOf(width - 40, 420)
    private val maxScroll get() = maxOf(0, rows.size * ROW_HEIGHT - (listBottom - listTop)).toDouble()

    override fun init() {
        shownVersion = -1L
        val y = height - 28
        addRenderableWidget(
            Button.builder(Component.translatable("screen.mc_tiny_tweak.baritone_log.clear")) { BaritoneLog.lines.clear() }
                .bounds(width / 2 - 155, y, 150, 20)
                .build(),
        )
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(width / 2 + 5, y, 150, 20).build())
    }

    private fun refresh() {
        val version = BaritoneLog.lines.version
        if (version == shownVersion) return
        shownVersion = version
        val time = font.width("00:00:00 ")
        rows = BaritoneLog.lines.snapshot().flatMap { entry ->
            val stamp = TIME.format(Instant.ofEpochMilli(entry.timeMillis))
            val wrapped = font.split(entry.value, rowWidth - time)
            wrapped.mapIndexed { i, line ->
                val prefix = if (i == 0) stamp else ""
                FormattedCharSequence.composite(
                    Component.literal(prefix.padEnd(9)).withStyle { it.withColor(TIME_COLOR) }.visualOrderText,
                    line,
                )
            }
        }
        if (following) scroll = maxScroll else scroll = scroll.coerceIn(0.0, maxScroll)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        scroll = (scroll - scrollY * ROW_HEIGHT * 3).coerceIn(0.0, maxScroll)
        following = scroll >= maxScroll
        return true
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        refresh()
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF.toInt())
        val left = (width - rowWidth) / 2
        graphics.fill(left - 4, listTop - 2, left + rowWidth + 4, listBottom + 2, 0x80000000.toInt())
        if (rows.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.mc_tiny_tweak.baritone_log.empty"), width / 2, listTop + 8, 0xFF808080.toInt())
            return
        }
        graphics.enableScissor(0, listTop, width, listBottom)
        rows.forEachIndexed { i, row ->
            val y = listTop + i * ROW_HEIGHT - scroll.toInt()
            if (y + ROW_HEIGHT < listTop || y > listBottom) return@forEachIndexed
            graphics.drawString(font, row, left, y + 1, 0xFFFFFFFF.toInt())
        }
        graphics.disableScissor()
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen() = false

    private companion object {
        const val ROW_HEIGHT = 11
        const val TIME_COLOR = 0x808080
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    }
}
