package dev.luniiya.mctinytweak.client.inventorypresets

import dev.luniiya.mctinytweak.inventorypresets.InventoryPresets
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.client.gui.screens.Screen
import org.lwjgl.glfw.GLFW

/** Asks for a preset's name, to save into a slot or to rename it. Goes back to the inventory either way. */
class InventoryPresetNameScreen(
    title: Component,
    private val initial: String,
    private val hint: Component,
    /** Shown under the name box, for example what saving replaces. */
    private val note: Component?,
    private val confirmLabel: Component,
    private val onConfirm: (String) -> Unit,
) : Screen(title) {
    private lateinit var nameBox: EditBox

    override fun init() {
        nameBox = object : EditBox(font, width / 2 - 100, height / 2 - 20, 200, 20, title) {
            override fun keyPressed(event: KeyEvent): Boolean {
                if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                    confirm()
                    return true
                }
                return super.keyPressed(event)
            }
        }
        nameBox.setMaxLength(InventoryPresets.MAX_NAME_LENGTH)
        nameBox.value = initial
        nameBox.setHint(hint.copy().withStyle(ChatFormatting.DARK_GRAY))
        addRenderableWidget(nameBox)
        setInitialFocus(nameBox)
        addRenderableWidget(Button.builder(confirmLabel) { confirm() }.bounds(width / 2 - 100, height / 2 + 24, 98, 20).build())
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL) { onClose() }.bounds(width / 2 + 2, height / 2 + 24, 98, 20).build())
    }

    private fun confirm() {
        onConfirm(nameBox.value)
        onClose()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 44, 0xFFFFFFFF.toInt())
        note?.let { graphics.drawCenteredString(font, it, width / 2, height / 2 + 6, 0xFFAAAAAA.toInt()) }
    }

    override fun onClose() {
        minecraft.setScreen(minecraft.player?.let(::InventoryScreen))
    }

    override fun isPauseScreen() = false
}
