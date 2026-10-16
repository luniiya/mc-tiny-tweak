package dev.luniiya.mctinytweak.client.itemeditor

import dev.luniiya.mctinytweak.itemeditor.ItemDraft
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/** Main item editor: item id, count, damage, unbreakable and name, with links to the detail editors. */
class ItemEditorScreen(
    private val session: ItemEditorSession,
    private val parent: Screen?,
) : Screen(Component.translatable("screen.mc_tiny_tweak.item_editor")) {
    private var status: Component? = null
    private lateinit var applyButton: Button

    private val left get() = (width - PANEL_WIDTH) / 2
    private val fieldsX get() = left + PREVIEW_SIZE + GAP
    private val top get() = height / 2 - 70

    override fun init() {
        val draft = session.draft
        var y = top

        textField(fieldsX, y, FIELD_WIDTH, "item", draft.itemId, 256) { session.draft = session.draft.copy(itemId = it) }
        y += ROW

        textField(fieldsX, y, 56, "count", draft.count.toString(), 2, digitsOnly = true) {
            session.draft = session.draft.copy(count = it.toIntOrNull() ?: 0)
        }
        textField(fieldsX + 60, y, 56, "damage", draft.damage.toString(), 5, digitsOnly = true) {
            session.draft = session.draft.copy(damage = it.toIntOrNull() ?: 0)
        }
        addRenderableWidget(
            CycleButton.onOffBuilder(draft.unbreakable).create(
                fieldsX + 120, y, FIELD_WIDTH - 120, 20, Component.translatable("screen.mc_tiny_tweak.item_editor.unbreakable"),
            ) { _, value -> session.draft = session.draft.copy(unbreakable = value) },
        )
        y += ROW

        textField(fieldsX, y, FIELD_WIDTH, "name", draft.name, 512) { session.draft = session.draft.copy(name = it) }
            .setTooltip(Tooltip.create(Component.translatable("screen.mc_tiny_tweak.item_editor.codes_hint")))
        y += ROW

        val third = (FIELD_WIDTH - 8) / 3
        addRenderableWidget(
            Button.builder(Component.translatable("screen.mc_tiny_tweak.item_editor.lore", draft.lore.size)) {
                minecraft.setScreen(LoreEditorScreen(session, this))
            }.bounds(fieldsX, y, third, 20).build(),
        )
        addRenderableWidget(
            Button.builder(Component.translatable("screen.mc_tiny_tweak.item_editor.enchantments", draft.enchantments.size)) {
                minecraft.setScreen(EnchantmentEditorScreen(session, this))
            }.bounds(fieldsX + third + 4, y, third, 20).build(),
        )
        addRenderableWidget(
            Button.builder(Component.translatable("screen.mc_tiny_tweak.item_editor.raw")) {
                minecraft.setScreen(RawItemEditorScreen(session, this))
            }.bounds(fieldsX + 2 * (third + 4), y, third, 20).build(),
        )

        val bottom = top + 4 * ROW + 24
        val half = (PANEL_WIDTH - 4) / 2
        applyButton = addRenderableWidget(
            Button.builder(Component.translatable("screen.mc_tiny_tweak.item_editor.apply")) {
                status = session.apply(minecraft)
                rebuildWidgets()
            }.bounds(left, bottom, half, 20).build(),
        )
        addRenderableWidget(Button.builder(Component.translatable("gui.done")) { onClose() }.bounds(left + half + 4, bottom, half, 20).build())
        refreshApplyButton()
    }

    private fun textField(
        x: Int, y: Int, w: Int, key: String, value: String, maxLength: Int,
        digitsOnly: Boolean = false, onChange: (String) -> Unit,
    ): EditBox {
        val label = Component.translatable("screen.mc_tiny_tweak.item_editor.$key")
        val box = EditBox(font, x, y, w, 20, label)
        box.setMaxLength(maxLength)
        if (digitsOnly) box.setFilter { text -> text.all(Char::isDigit) }
        box.value = value
        box.setHint(label.copy().withStyle(ChatFormatting.DARK_GRAY))
        box.setResponder {
            onChange(it)
            status = null
            refreshApplyButton()
        }
        return addRenderableWidget(box)
    }

    private fun refreshApplyButton() {
        val creative = minecraft.player?.hasInfiniteMaterials() == true
        applyButton.active = creative && session.result is ItemDraft.Result.Success
        applyButton.setTooltip(if (creative) null else Tooltip.create(Component.translatable("screen.mc_tiny_tweak.item_editor.creative_only")))
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, top - 24, 0xFFFFFFFF.toInt())

        val previewY = top + 4
        graphics.fill(left, previewY, left + PREVIEW_SIZE, previewY + PREVIEW_SIZE, 0x66000000)
        graphics.pose().pushMatrix()
        graphics.pose().translate((left + 8).toFloat(), (previewY + 8).toFloat())
        graphics.pose().scale(3f, 3f)
        graphics.renderItem(session.preview, 0, 0)
        graphics.renderItemDecorations(font, session.preview, 0, 0)
        graphics.pose().popMatrix()
        if (mouseX in left until left + PREVIEW_SIZE && mouseY in previewY until previewY + PREVIEW_SIZE) {
            graphics.setTooltipForNextFrame(font, session.preview, mouseX, mouseY)
        }

        val messageY = top + 4 * ROW + 8
        val result = session.result
        val message = when {
            result is ItemDraft.Result.Failure -> Component.literal(result.errors.first()).withStyle(ChatFormatting.RED)
            else -> status
        }
        message?.let { graphics.drawCenteredString(font, it, width / 2, messageY, 0xFFFFFFFF.toInt()) }
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen() = false

    companion object {
        private const val PREVIEW_SIZE = 64
        private const val GAP = 8
        private const val FIELD_WIDTH = 228
        private const val PANEL_WIDTH = PREVIEW_SIZE + GAP + FIELD_WIDTH
        private const val ROW = 24

        fun forHeldItem(minecraft: Minecraft, parent: Screen?) = ItemEditorScreen(ItemEditorSession.forHeldItem(minecraft), parent)
    }
}
