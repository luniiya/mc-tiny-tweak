package dev.luniiya.mctinytweak.client.itemeditor

import dev.luniiya.mctinytweak.itemeditor.EnchantmentLevels
import dev.luniiya.mctinytweak.itemeditor.ItemDraft
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items

/**
 * Lists every registered enchantment with a level box; empty or 0 means not applied.
 * Enchantments that fit the edited item come first; the rest are gray but still work.
 */
class EnchantmentEditorScreen(
    private val session: ItemEditorSession,
    private val parent: Screen,
) : Screen(Component.translatable("screen.mc_tiny_tweak.enchantment_editor")) {
    private data class Entry(val id: String, val name: Component, val maxLevel: Int, val compatible: Boolean)

    private val all: List<Entry> = run {
        val item = session.preview
        // Books take every enchantment, but no enchantment lists them as a supported item.
        val book = item.`is`(Items.BOOK) || item.`is`(Items.ENCHANTED_BOOK)
        val entries = session.registries.lookupOrThrow(Registries.ENCHANTMENT).listElements()
            .map { Entry(it.key().identifier().toString(), it.value().description(), it.value().maxLevel, book || it.value().canEnchant(item)) }
            .toList()
        EnchantmentLevels.compatibleFirst(entries, Entry::compatible) { it.name.string }
    }

    private var levels: Map<String, Int> = session.draft.enchantments
    private var query = ""
    private var page = 0
    private var shown: List<Entry> = all
    private var binding = false
    private val rows = mutableListOf<Row>()
    private lateinit var previous: Button
    private lateinit var next: Button

    private class Row(val box: EditBox, val one: Button, val max: Button, val maxPlus: Button, val clear: Button) {
        val widgets get() = listOf(box, one, max, maxPlus, clear)
    }

    /** As many rows as fit between the search bar and the footer, so small windows still show Done. */
    private val rowCount get() = ((height - TOP - 28 - 54) / ROW_HEIGHT).coerceIn(3, 10)
    private val left get() = width / 2 - WIDTH / 2
    private val pageCount get() = maxOf(1, (shown.size + rowCount - 1) / rowCount)

    override fun init() {
        val search = EditBox(font, left, TOP, WIDTH, 20, Component.translatable("gui.recipebook.search_hint"))
        search.value = query
        search.setHint(Component.translatable("gui.recipebook.search_hint").withStyle(ChatFormatting.DARK_GRAY))
        search.setResponder {
            query = it
            page = 0
            refilter()
        }
        addRenderableWidget(search)

        rows.clear()
        val controls = BOX_WIDTH + 2 + 20 + 2 + 32 + 2 + 36 + 2 + 40
        for (i in 0 until rowCount) {
            val y = TOP + 28 + i * ROW_HEIGHT
            var x = left + WIDTH - controls
            val box = EditBox(font, x, y + 1, BOX_WIDTH, 18, Component.translatable("screen.mc_tiny_tweak.enchantment_editor.level"))
            box.setMaxLength(3)
            box.setFilter { text -> text.all(Char::isDigit) }
            box.setResponder { text ->
                if (!binding) visible().getOrNull(i)?.let { levels = EnchantmentLevels.with(levels, it.id, text.toIntOrNull() ?: 0) }
            }
            x += BOX_WIDTH + 2
            val one = Button.builder(Component.literal("1")) { box.value = "1" }.bounds(x, y, 20, 20).build()
            x += 22
            val max = Button.builder(Component.translatable("screen.mc_tiny_tweak.enchantment_editor.max_button")) {
                visible().getOrNull(i)?.let { box.value = it.maxLevel.toString() }
            }.bounds(x, y, 32, 20).build()
            x += 34
            val maxPlus = Button.builder(Component.translatable("screen.mc_tiny_tweak.enchantment_editor.max_plus")) {
                box.value = ItemDraft.MAX_ENCHANTMENT_LEVEL.toString()
            }.bounds(x, y, 36, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.mc_tiny_tweak.enchantment_editor.max_plus.tooltip", ItemDraft.MAX_ENCHANTMENT_LEVEL)))
                .build()
            x += 38
            val clear = Button.builder(Component.translatable("screen.mc_tiny_tweak.enchantment_editor.clear")) { box.value = "" }
                .bounds(x, y, 40, 20).build()
            val row = Row(box, one, max, maxPlus, clear)
            row.widgets.forEach { addRenderableWidget(it) }
            rows += row
        }

        val footer = TOP + 28 + rowCount * ROW_HEIGHT + 4
        previous = addRenderableWidget(Button.builder(Component.literal("<")) { page--; bindRows() }.bounds(left, footer, 20, 20).build())
        next = addRenderableWidget(Button.builder(Component.literal(">")) { page++; bindRows() }.bounds(left + WIDTH - 20, footer, 20, 20).build())
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

    private fun visible(): List<Entry> = shown.drop(page * rowCount).take(rowCount)

    private fun bindRows() {
        page = page.coerceIn(0, pageCount - 1)
        val entries = visible()
        binding = true
        rows.forEachIndexed { i, row ->
            val entry = entries.getOrNull(i)
            row.widgets.forEach { it.visible = entry != null }
            row.box.value = entry?.let { levels[it.id]?.toString() }.orEmpty()
            if (entry != null) {
                val tip = Component.translatable("screen.mc_tiny_tweak.enchantment_editor.level.tooltip", ItemDraft.MAX_ENCHANTMENT_LEVEL, entry.maxLevel)
                if (!entry.compatible) {
                    tip.append("\n").append(Component.translatable("screen.mc_tiny_tweak.enchantment_editor.incompatible").withStyle(ChatFormatting.GRAY))
                }
                row.box.setTooltip(Tooltip.create(tip))
                row.max.setTooltip(Tooltip.create(Component.translatable("screen.mc_tiny_tweak.enchantment_editor.max_button.tooltip", entry.maxLevel)))
            }
        }
        binding = false
        previous.active = page > 0
        next.active = page < pageCount - 1
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF.toInt())
        visible().forEachIndexed { i, entry ->
            val color = when {
                (levels[entry.id] ?: 0) > 0 -> 0xFFFFD866.toInt()
                entry.compatible -> 0xFFE0E0E0.toInt()
                else -> 0xFF808080.toInt()
            }
            graphics.drawString(font, entry.name, left, TOP + 34 + i * ROW_HEIGHT, color)
        }
        val footer = TOP + 28 + rowCount * ROW_HEIGHT + 4
        graphics.drawCenteredString(font, Component.literal("${page + 1} / $pageCount"), width / 2, footer + 6, 0xFFA0A0A0.toInt())
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen() = false

    private companion object {
        const val TOP = 36
        const val ROW_HEIGHT = 22
        const val WIDTH = 360
        const val BOX_WIDTH = 36
    }
}
