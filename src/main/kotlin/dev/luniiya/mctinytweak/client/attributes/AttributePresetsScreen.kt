package dev.luniiya.mctinytweak.client.attributes

import dev.luniiya.mctinytweak.attributes.AttributePresets
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
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Save your current base attributes under a name, or load a saved set. Default resets everything. */
class AttributePresetsScreen(private val parent: Screen?) : Screen(Component.translatable("screen.mc_tiny_tweak.attributes.presets")) {
    private lateinit var nameBox: EditBox
    private lateinit var saveButton: Button
    private lateinit var list: PresetList
    private var status: Component = Component.empty()

    override fun init() {
        nameBox = EditBox(font, width / 2 - 150, 22, 240, 20, Component.translatable("screen.mc_tiny_tweak.attributes.presets.name"))
        nameBox.setHint(Component.translatable("screen.mc_tiny_tweak.attributes.presets.name").withStyle(ChatFormatting.DARK_GRAY))
        nameBox.setMaxLength(64)
        nameBox.setResponder { saveButton.active = canSave() }
        saveButton = Button.builder(Component.translatable("screen.mc_tiny_tweak.attributes.presets.save")) { save() }
            .bounds(width / 2 + 94, 22, 56, 20)
            .tooltip(Tooltip.create(Component.translatable("screen.mc_tiny_tweak.attributes.presets.save.tooltip")))
            .build()
        saveButton.active = canSave()
        list = PresetList(minecraft, width, height - 50 - 44, 50)
        list.refill()
        addRenderableWidget(list)
        addRenderableWidget(nameBox)
        addRenderableWidget(saveButton)
        setInitialFocus(nameBox)
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(width / 2 - 100, height - 26, 200, 20).build())
    }

    private fun canSave() = minecraft.player != null && PlayerAttributes.presets().canSave(nameBox.value)

    private fun save() {
        val player = minecraft.player ?: return
        if (!canSave()) return
        val name = nameBox.value.trim()
        PlayerAttributes.update { it.save(name, PlayerAttributes.baseValues(player)) }
        status = Component.translatable("screen.mc_tiny_tweak.attributes.presets.saved", name)
        nameBox.value = ""
        list.refill()
    }

    private fun load(name: String) {
        val player = minecraft.player ?: return
        val sent = PlayerAttributes.load(player, name)
        status = Component.translatable("screen.mc_tiny_tweak.attributes.presets.loaded", name, sent)
    }

    private fun delete(name: String) {
        minecraft.setScreen(
            ConfirmScreen(
                { confirmed ->
                    if (confirmed) {
                        PlayerAttributes.update { it.delete(name) }
                        status = Component.translatable("screen.mc_tiny_tweak.attributes.presets.deleted", name)
                    }
                    minecraft.setScreen(this)
                },
                Component.translatable("screen.mc_tiny_tweak.attributes.presets.delete"),
                Component.translatable("screen.mc_tiny_tweak.attributes.presets.delete.confirm", name),
            ),
        )
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF.toInt())
        val message = if (ItemEditorSession.canApplyAsOperator(minecraft)) status
        else Component.translatable("screen.mc_tiny_tweak.attributes.operator_only").withStyle(ChatFormatting.RED)
        graphics.drawCenteredString(font, message, width / 2, height - 38, 0xFFAAAAAA.toInt())
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    override fun isPauseScreen() = false

    private inner class PresetList(minecraft: Minecraft, width: Int, height: Int, y: Int) :
        ContainerObjectSelectionList<Row>(minecraft, width, height, y, 24) {
        fun refill() {
            replaceEntries(PlayerAttributes.presets().names.map(::Row))
            setScrollAmount(0.0)
        }

        override fun getRowWidth() = 300
    }

    private inner class Row(private val name: String) : ContainerObjectSelectionList.Entry<Row>() {
        private val builtIn = PlayerAttributes.presets().isBuiltIn(name)
        // Built-in presets are gray, like other things you cannot change here.
        private val label: Component = if (name == AttributePresets.DEFAULT) {
            Component.translatable("screen.mc_tiny_tweak.attributes.presets.default").withStyle(ChatFormatting.GRAY)
        } else {
            Component.literal(name).withStyle(if (builtIn) ChatFormatting.GRAY else ChatFormatting.WHITE)
        }
        private val loadButton = Button.builder(Component.translatable("screen.mc_tiny_tweak.attributes.presets.load")) { load(name) }
            .size(50, 20)
            .apply { if (name == AttributePresets.DEFAULT) tooltip(Tooltip.create(Component.translatable("screen.mc_tiny_tweak.attributes.presets.default.tooltip"))) }
            .build()
            .also { it.active = ItemEditorSession.canApplyAsOperator(minecraft) }
        private val deleteButton = Button.builder(Component.translatable("screen.mc_tiny_tweak.attributes.presets.delete")) { delete(name) }
            .size(50, 20).build()
        private val widgets = if (builtIn) listOf(loadButton) else listOf(loadButton, deleteButton)

        override fun renderContent(graphics: GuiGraphics, mouseX: Int, mouseY: Int, hovered: Boolean, partialTick: Float) {
            val text = font.substrByWidth(label, contentWidth - 110)
            graphics.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(text), contentX, contentYMiddle - 4, 0xFFFFFFFF.toInt())
            deleteButton.setPosition(contentRight - 50, contentY)
            loadButton.setPosition(contentRight - 104, contentY)
            widgets.forEach { it.render(graphics, mouseX, mouseY, partialTick) }
        }

        override fun children(): List<GuiEventListener> = widgets

        override fun narratables(): List<NarratableEntry> = widgets
    }
}
