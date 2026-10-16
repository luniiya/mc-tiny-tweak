package dev.luniiya.mctinytweak.client.itemeditor

import dev.luniiya.mctinytweak.itemeditor.ItemDraft
import dev.luniiya.mctinytweak.itemeditor.ItemSnbt
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.MultiLineEditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/** Edits the whole item, including every data component, as SNBT. A catch-all until dedicated screens exist. */
class RawItemEditorScreen(
    private val session: ItemEditorSession,
    private val parent: Screen,
) : Screen(Component.translatable("screen.mc_tiny_tweak.raw_editor")) {
    private var text: String = ItemSnbt.encode(session.preview, session.registries)
    private var error: String? = null

    override fun init() {
        val box = MultiLineEditBox.builder()
            .setX(MARGIN)
            .setY(TOP)
            .build(font, width - 2 * MARGIN, height - TOP - 52, title)
        box.setCharacterLimit(MAX_CHARACTERS)
        box.setValue(text)
        box.setValueListener {
            text = it
            error = null
        }
        addRenderableWidget(box)

        addRenderableWidget(
            Button.builder(Component.translatable("gui.done")) {
                when (val result = ItemSnbt.decode(text, session.registries)) {
                    is ItemDraft.Result.Success -> {
                        session.reset(result.stack)
                        onClose()
                    }
                    is ItemDraft.Result.Failure -> error = result.errors.joinToString("; ")
                }
            }.bounds(width / 2 - 102, height - 28, 100, 20).build(),
        )
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel")) { onClose() }.bounds(width / 2 + 2, height - 28, 100, 20).build())
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF.toInt())
        error?.let {
            val line = font.plainSubstrByWidth(it, width - 2 * MARGIN)
            graphics.drawCenteredString(font, Component.literal(line).withStyle(ChatFormatting.RED), width / 2, height - 44, 0xFFFFFFFF.toInt())
        }
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen() = false

    private companion object {
        const val MARGIN = 16
        const val TOP = 28
        const val MAX_CHARACTERS = 262_144
    }
}
