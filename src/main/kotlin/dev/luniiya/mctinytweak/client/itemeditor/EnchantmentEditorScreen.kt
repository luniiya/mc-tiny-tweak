package dev.luniiya.mctinytweak.client.itemeditor

import dev.luniiya.mctinytweak.itemeditor.EnchantmentLevels
import dev.luniiya.mctinytweak.itemeditor.ItemDraft
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component

/** Lists every registered enchantment with a level box; empty or 0 means not applied. */
class EnchantmentEditorScreen(
    private val session: ItemEditorSession,
    private val parent: Screen,
) : Screen(Component.translatable("screen.mc_tiny_tweak.enchantment_editor")) {
    private data class Entry(val id: String, val name: Component)

    private val all: List<Entry> = session.registries.lookupOrThrow(Registries.ENCHANTMENT).listElements()
        .map { Entry(it.key().identifier().toString(), it.value().description()) }
        .toList()
        .sortedBy { it.id }

    private var levels: Map<String, Int> = session.draft.enchantments
    private var query = ""
    private var page = 0
    private var shown: List<Entry> = all
    private var binding = false
    private val rows = mutableListOf<EditBox>()
    private lateinit var previous: Button
    private lateinit var next: Button

    private val left get() = width / 2 - 130
    private val pageCount get() = maxOf(1, (shown.size + ROWS - 1) / ROWS)

    override fun init() {
        val search = EditBox(font, left, TOP, 260, 20, Component.translatable("gui.recipebook.search_hint"))
        search.value = query
        search.setHint(Component.translatable("gui.recipebook.search_hint").withStyle(ChatFormatting.DARK_GRAY))
        search.setResponder {
            query = it
            page = 0
            refilter()
        }
        addRenderableWidget(search)

        rows.clear()
        for (i in 0 until ROWS) {
            val box = EditBox(font, left + 220, TOP + 28 + i * ROW_HEIGHT, 40, 18, Component.translatable("screen.mc_tiny_tweak.enchantment_editor.level"))
            box.setMaxLength(3)
            box.setFilter { text -> text.all(Char::isDigit) }
            box.setResponder { text ->
                if (!binding) visible().getOrNull(i)?.let { levels = EnchantmentLevels.with(levels, it.id, text.toIntOrNull() ?: 0) }
            }
            rows += addRenderableWidget(box)
        }

        val footer = TOP + 28 + ROWS * ROW_HEIGHT + 4
        previous = addRenderableWidget(Button.builder(Component.literal("<")) { page--; bindRows() }.bounds(left, footer, 20, 20).build())
        next = addRenderableWidget(Button.builder(Component.literal(">")) { page++; bindRows() }.bounds(left + 240, footer, 20, 20).build())
        addRenderableWidget(
            Button.builder(Component.translatable("gui.done")) {
                session.draft = session.draft.copy(enchantments = levels)
                onClose()
            }.bounds(width / 2 - 102, footer + 26, 100, 20).build(),
        )
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel")) { onClose() }.bounds(width / 2 + 2, footer + 26, 100, 20).build())
        refilter()
    }

    private fun refilter() {
        shown = all.filter { EnchantmentLevels.matches(query, it.id, it.name.string) }
        bindRows()
    }

    private fun visible(): List<Entry> = shown.drop(page * ROWS).take(ROWS)

    private fun bindRows() {
        page = page.coerceIn(0, pageCount - 1)
        val entries = visible()
        binding = true
        rows.forEachIndexed { i, box ->
            val entry = entries.getOrNull(i)
            box.visible = entry != null
            box.value = entry?.let { levels[it.id]?.toString() }.orEmpty()
        }
        binding = false
        previous.active = page > 0
        next.active = page < pageCount - 1
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF.toInt())
        visible().forEachIndexed { i, entry ->
            val active = (levels[entry.id] ?: 0) > 0
            val color = if (active) 0xFFFFD866.toInt() else 0xFFE0E0E0.toInt()
            graphics.drawString(font, entry.name, left, TOP + 33 + i * ROW_HEIGHT, color)
        }
        val footer = TOP + 28 + ROWS * ROW_HEIGHT + 4
        graphics.drawCenteredString(font, Component.literal("${page + 1} / $pageCount"), width / 2, footer + 6, 0xFFA0A0A0.toInt())
        graphics.drawString(
            font,
            Component.translatable("screen.mc_tiny_tweak.enchantment_editor.max", ItemDraft.MAX_ENCHANTMENT_LEVEL),
            left, TOP + 22 - font.lineHeight, 0xFF808080.toInt(),
        )
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen() = false

    private companion object {
        const val TOP = 36
        const val ROWS = 8
        const val ROW_HEIGHT = 20
    }
}
