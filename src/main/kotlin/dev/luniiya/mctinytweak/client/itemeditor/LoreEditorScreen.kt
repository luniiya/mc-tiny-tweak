package dev.luniiya.mctinytweak.client.itemeditor

import dev.luniiya.mctinytweak.itemeditor.LegacyText
import dev.luniiya.mctinytweak.itemeditor.LoreLines
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.MultiLineEditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.item.component.ItemLore

/** Edits lore as one text line per lore line, with a live styled preview beside it. */
class LoreEditorScreen(
    private val session: ItemEditorSession,
    private val parent: Screen,
) : Screen(Component.translatable("screen.mc_tiny_tweak.lore_editor")) {
    private var text: String = LoreLines.join(session.draft.lore)

    override fun init() {
        val boxWidth = (width - 3 * MARGIN) / 2
        val box = MultiLineEditBox.builder()
            .setX(MARGIN)
            .setY(TOP)
            .setPlaceholder(Component.translatable("screen.mc_tiny_tweak.item_editor.codes_hint"))
            .build(font, boxWidth, height - TOP - 40, title)
        box.setLineLimit(ItemLore.MAX_LINES)
        box.setValue(text)
        box.setValueListener { text = it }
        addRenderableWidget(box)

        val half = 100
        addRenderableWidget(
            Button.builder(Component.translatable("gui.done")) {
                session.draft = session.draft.copy(lore = LoreLines.split(text))
                onClose()
            }.bounds(width / 2 - half - 2, height - 28, half, 20).build(),
        )
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel")) { onClose() }.bounds(width / 2 + 2, height - 28, half, 20).build())
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF.toInt())
        val previewX = width / 2 + MARGIN / 2
        var y = TOP + 4
        for (line in LoreLines.split(text)) {
            if (y > height - 48) break
            // Unset colors fall back to the purple lore color, matching the in-game tooltip.
            graphics.drawString(font, Component.empty().withStyle(LORE_STYLE).append(LegacyText.parse(line)), previewX, y, 0xFFFFFFFF.toInt())
            y += font.lineHeight + 2
        }
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen() = false

    private companion object {
        const val MARGIN = 16
        const val TOP = 32
        val LORE_STYLE: Style = Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true)
    }
}
