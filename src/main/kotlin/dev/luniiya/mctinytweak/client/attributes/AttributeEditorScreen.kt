package dev.luniiya.mctinytweak.client.attributes

import dev.luniiya.mctinytweak.attributes.AttributeAutoApply
import dev.luniiya.mctinytweak.attributes.AttributeCommands
import dev.luniiya.mctinytweak.client.itemeditor.ItemEditorSession
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.core.Holder
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.RangedAttribute
import org.lwjgl.glfw.GLFW

/** Every attribute of your own player. Set and Reset send `/attribute @s`, so they need operator rights. */
class AttributeEditorScreen(private val parent: Screen?) : Screen(Component.translatable("screen.mc_tiny_tweak.attributes")) {
    private var query = ""
    private lateinit var list: AttributeList

    override fun init() {
        val search = EditBox(font, width / 2 - 180, 22, 360, 18, Component.translatable("screen.mc_tiny_tweak.attributes.search"))
        search.setHint(Component.translatable("screen.mc_tiny_tweak.attributes.search").withStyle(ChatFormatting.DARK_GRAY))
        search.value = query
        search.setResponder {
            query = it
            list.refill()
        }
        list = AttributeList(minecraft, width, height - 46 - 32, 46)
        list.refill()
        addRenderableWidget(list)
        addRenderableWidget(search)
        setInitialFocus(search)
        addRenderableWidget(
            Button.builder(Component.translatable("screen.mc_tiny_tweak.attributes.presets")) { minecraft.setScreen(AttributePresetsScreen(this)) }
                .bounds(width / 2 - 155, height - 26, 150, 20)
                .build(),
        )
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(width / 2 + 5, height - 26, 150, 20).build())
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF.toInt())
        if (!ItemEditorSession.canApplyAsOperator(minecraft)) {
            graphics.drawCenteredString(font, Component.translatable("screen.mc_tiny_tweak.attributes.operator_only"), width / 2, height - 38, 0xFFFF5555.toInt())
        }
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen() = false

    private inner class AttributeList(minecraft: Minecraft, width: Int, height: Int, y: Int) :
        ContainerObjectSelectionList<Row>(minecraft, width, height, y, 24) {
        fun refill() {
            val rows = minecraft.player?.let(PlayerAttributes::holders).orEmpty()
                .map { Row(it) }
                .filter { it.matches(query) }
                .sortedBy { it.name.string }
            replaceEntries(rows)
            setScrollAmount(0.0)
        }

        override fun getRowWidth() = 360
    }

    private inner class Row(private val holder: Holder<Attribute>) : ContainerObjectSelectionList.Entry<Row>() {
        private val id = holder.registeredName
        val name: Component = Component.translatable(holder.value().descriptionId)
        private val synced = holder.value().isClientSyncable
        private val range = (holder.value() as? RangedAttribute)?.let { it.minValue..it.maxValue }
            ?: Double.NEGATIVE_INFINITY..Double.POSITIVE_INFINITY
        private val default = PlayerAttributes.default(holder)
        private val operator = ItemEditorSession.canApplyAsOperator(minecraft)

        private val input: EditBox = object : EditBox(font, 0, 0, 64, 18, name) {
            override fun keyPressed(event: KeyEvent): Boolean {
                if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                    set()
                    return true
                }
                return super.keyPressed(event)
            }
        }.apply {
            setMaxLength(32)
            value = AttributeCommands.format(base())
            moveCursorToStart(false)
            setResponder { setTextColor(if (parsed() != null) 0xFFE0E0E0.toInt() else 0xFFFF5555.toInt()) }
        }
        private val setButton = Button.builder(Component.translatable("screen.mc_tiny_tweak.attributes.set")) { set() }
            .size(34, 20).build()
        private val resetButton = Button.builder(Component.translatable("controls.reset")) { reset() }
            .size(RESET_WIDTH, 20)
            .tooltip(Tooltip.create(Component.translatable("screen.mc_tiny_tweak.attributes.reset", AttributeCommands.format(default))))
            .build()
        private val widgets = listOf(input, setButton, resetButton)

        init {
            input.setTooltip(
                Tooltip.create(
                    Component.translatable(
                        if (synced) "screen.mc_tiny_tweak.attributes.tooltip" else "screen.mc_tiny_tweak.attributes.tooltip.unsynced",
                        id,
                        AttributeCommands.format(range.start),
                        AttributeCommands.format(range.endInclusive),
                        AttributeCommands.format(default),
                    ),
                ),
            )
            setButton.active = operator
        }

        fun matches(query: String): Boolean {
            val q = query.trim().lowercase()
            return q.isEmpty() || q in name.string.lowercase() || q in id.lowercase()
        }

        private fun instance() = minecraft.player?.attributes?.getInstance(holder)

        private fun base(): Double = instance()?.baseValue ?: default

        private fun parsed(): Double? = AttributeCommands.parse(input.value, range.start, range.endInclusive)

        private fun set() {
            val value = parsed() ?: return
            val player = minecraft.player ?: return
            if (!operator) return
            player.connection.sendCommand(AttributeCommands.setBase(id, value))
            AttributeAutoApplier.remember(minecraft, AttributeAutoApply.CUSTOM)
            // Synced attributes come back from the server; this keeps unsynced ones showing what was set.
            instance()?.baseValue = value
        }

        private fun reset() {
            val player = minecraft.player ?: return
            if (!operator) return
            player.connection.sendCommand(AttributeCommands.resetBase(id))
            AttributeAutoApplier.remember(minecraft, AttributeAutoApply.CUSTOM)
            player.attributes.resetBaseValue(holder)
            input.value = AttributeCommands.format(default)
        }

        override fun renderContent(graphics: GuiGraphics, mouseX: Int, mouseY: Int, hovered: Boolean, partialTick: Float) {
            val changed = base() != default
            resetButton.active = operator && changed
            val color = when {
                changed -> 0xFFFFFF55.toInt()
                synced -> 0xFFFFFFFF.toInt()
                else -> 0xFFAAAAAA.toInt()
            }
            val controlsWidth = 64 + 4 + 34 + 2 + RESET_WIDTH
            val valueText = AttributeCommands.format(instance()?.value ?: default).let { if (synced) it else "$it*" }
            val valueWidth = 56
            val label = font.substrByWidth(name, contentWidth - controlsWidth - valueWidth - 8)
            graphics.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(label), contentX, contentYMiddle - 4, color)
            graphics.drawString(font, font.plainSubstrByWidth(valueText, valueWidth), contentRight - controlsWidth - valueWidth - 4, contentYMiddle - 4, 0xFF888888.toInt())
            var x = contentRight - controlsWidth
            input.setPosition(x, contentY + 1)
            x += 64 + 4
            setButton.setPosition(x, contentY)
            x += 34 + 2
            resetButton.setPosition(x, contentY)
            widgets.forEach { it.render(graphics, mouseX, mouseY, partialTick) }
        }

        override fun children(): List<GuiEventListener> = widgets

        override fun narratables(): List<NarratableEntry> = widgets
    }

    private companion object {
        const val RESET_WIDTH = 40
    }
}
