package dev.luniiya.mctinytweak.client.baritone

import dev.luniiya.mctinytweak.baritone.BaritoneDefaults
import dev.luniiya.mctinytweak.client.mcp.BaritoneSettings
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Every Baritone setting with a search box. Reset goes back to the mod's Baritone defaults. */
class BaritoneSettingsScreen(private val parent: Screen?) : Screen(Component.translatable("screen.mc_tiny_tweak.baritone")) {
    private var query = ""
    private lateinit var list: SettingList

    override fun init() {
        val search = EditBox(font, width / 2 - 155, 22, 310, 18, Component.translatable("screen.mc_tiny_tweak.baritone.search"))
        search.setHint(Component.translatable("screen.mc_tiny_tweak.baritone.search").withStyle(ChatFormatting.DARK_GRAY))
        search.value = query
        search.setResponder {
            query = it
            list.refill()
        }
        list = SettingList(minecraft, width, height - 46 - 32, 46)
        list.refill()
        addRenderableWidget(list)
        addRenderableWidget(search)
        setInitialFocus(search)
        val y = height - 26
        addRenderableWidget(
            Button.builder(Component.translatable("screen.mc_tiny_tweak.baritone.reset")) { confirmReset() }
                .bounds(width / 2 - 155, y, 150, 20)
                .tooltip(Tooltip.create(Component.translatable("screen.mc_tiny_tweak.baritone.reset.tooltip")))
                .build(),
        )
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(width / 2 + 5, y, 150, 20).build())
    }

    private fun confirmReset() {
        minecraft.setScreen(
            ConfirmScreen(
                { confirmed ->
                    if (confirmed) BaritoneSettings.resetToModDefaults()
                    minecraft.setScreen(this)
                },
                Component.translatable("screen.mc_tiny_tweak.baritone.reset"),
                Component.translatable("screen.mc_tiny_tweak.baritone.reset.confirm"),
            ),
        )
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF.toInt())
    }

    override fun onClose() {
        BaritoneSettings.save()
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen() = false

    private inner class SettingList(minecraft: Minecraft, width: Int, height: Int, y: Int) :
        ContainerObjectSelectionList<Row>(minecraft, width, height, y, 24) {
        fun refill() {
            replaceEntries(BaritoneSettings.entries().filter { BaritoneDefaults.matches(it.name, query) }.map(::Row))
            setScrollAmount(0.0)
        }

        override fun getRowWidth() = 310
    }

    private inner class Row(entry: BaritoneSettings.Entry) : ContainerObjectSelectionList.Entry<Row>() {
        private val name = entry.name
        private var modified = entry.modified
        private val label = Component.literal(entry.name)
        private val widget: AbstractWidget = if (entry.isBoolean) {
            CycleButton.onOffBuilder(entry.value == "true").displayOnlyValue()
                .create(0, 0, 120, 20, label) { _, value -> apply(value.toString()) }
        } else {
            EditBox(font, 0, 0, 120, 18, label).apply {
                setMaxLength(4096)
                value = entry.value
                moveCursorToStart(false)
                setResponder { text ->
                    val valid = runCatching { apply(text) }.isSuccess
                    setTextColor(if (valid) 0xFFE0E0E0.toInt() else 0xFFFF5555.toInt())
                }
            }
        }

        init {
            widget.setTooltip(Tooltip.create(Component.translatable("screen.mc_tiny_tweak.baritone.setting.tooltip", entry.name, entry.type, entry.default)))
        }

        private fun apply(value: String) {
            BaritoneSettings.set(name, value, save = false)
            modified = BaritoneSettings.entry(name)?.modified ?: false
        }

        override fun renderContent(graphics: GuiGraphics, mouseX: Int, mouseY: Int, hovered: Boolean, partialTick: Float) {
            val color = if (modified) 0xFFFFFF55.toInt() else 0xFFFFFFFF.toInt()
            val text = font.substrByWidth(label, contentWidth - 130)
            graphics.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(text), contentX, contentYMiddle - 4, color)
            widget.setPosition(contentRight - 120, contentY + (if (widget is EditBox) 1 else 0))
            widget.render(graphics, mouseX, mouseY, partialTick)
        }

        override fun children(): List<GuiEventListener> = listOf(widget)

        override fun narratables(): List<NarratableEntry> = listOf(widget)
    }
}
