package dev.luniiya.mctinytweak.client.inventorypresets

import dev.luniiya.mctinytweak.client.itemeditor.ItemEditorSession
import dev.luniiya.mctinytweak.inventorypresets.InventoryPresets
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemStack

/**
 * A hotbar of [InventoryPresets.SIZE] whole-inventory presets at the bottom of the survival inventory screen, only for operators.
 * Left click loads a preset, right click saves the current inventory into it.
 */
class InventoryPresetBar(private val screen: InventoryScreen) :
    AbstractWidget(0, 0, WIDTH, HEIGHT, Component.translatable("screen.mc_tiny_tweak.inventory_presets")) {
    private val minecraft = Minecraft.getInstance()

    init {
        place()
    }

    /** Sits exactly where the HUD hotbar is: centered at the bottom of the screen. */
    private fun place() {
        x = screen.width / 2 - WIDTH / 2
        y = screen.height - HEIGHT
    }

    /** The preset slot under the mouse, or -1. */
    private fun slotAt(mouseX: Double, mouseY: Double): Int {
        if (!isMouseOver(mouseX, mouseY)) return -1
        return ((mouseX - x - 1) / SLOT_STEP).toInt().coerceIn(0, InventoryPresets.SIZE - 1)
    }

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        place()
        val player = minecraft.player ?: return
        val presets = InventoryPresetStore.presets(player)
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, x, y, WIDTH, HEIGHT)
        for (i in 0 until InventoryPresets.SIZE) {
            graphics.renderItem(presets.icon(i), x + 3 + i * SLOT_STEP, y + 3)
        }
        val hovered = slotAt(mouseX.toDouble(), mouseY.toDouble())
        if (hovered >= 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SELECTION_SPRITE, x - 1 + hovered * SLOT_STEP, y - 1, 24, 23)
            graphics.setTooltipForNextFrame(tooltip(hovered, presets[hovered]), mouseX, mouseY)
        }
    }

    private fun tooltip(index: Int, slots: Map<String, ItemStack>?): List<FormattedCharSequence> {
        val lines = mutableListOf<Component>(Component.translatable("screen.mc_tiny_tweak.inventory_presets.slot", index + 1))
        if (slots == null) {
            lines += Component.translatable("screen.mc_tiny_tweak.inventory_presets.empty").withStyle(ChatFormatting.GRAY)
            lines += Component.translatable("screen.mc_tiny_tweak.inventory_presets.hint_save").withStyle(ChatFormatting.DARK_GRAY)
        } else {
            val stacks = InventoryPresets.SLOT_NAMES.mapNotNull { slots[it] }
            stacks.take(MAX_TOOLTIP_ITEMS).forEach { stack ->
                lines += Component.literal("${stack.count}× ").withStyle(ChatFormatting.GRAY).append(stack.hoverName)
            }
            if (stacks.size > MAX_TOOLTIP_ITEMS) {
                lines += Component.translatable("screen.mc_tiny_tweak.inventory_presets.more", stacks.size - MAX_TOOLTIP_ITEMS)
                    .withStyle(ChatFormatting.GRAY)
            }
            lines += Component.translatable("screen.mc_tiny_tweak.inventory_presets.hint_load").withStyle(ChatFormatting.DARK_GRAY)
            lines += Component.translatable("screen.mc_tiny_tweak.inventory_presets.hint_save_over").withStyle(ChatFormatting.DARK_GRAY)
        }
        return lines.map { it.visualOrderText }
    }

    override fun isValidClickButton(info: MouseButtonInfo) = info.button() == 0 || info.button() == 1

    override fun onClick(event: MouseButtonEvent, doubleClick: Boolean) {
        val player = minecraft.player ?: return
        val index = slotAt(event.x(), event.y()).takeIf { it >= 0 } ?: return
        val presets = InventoryPresetStore.presets(player)
        val current = InventoryPresetStore.snapshot(player)
        if (event.button() == 1) {
            val existing = presets[index]
            if (existing == null || InventoryPresets.sameSlots(existing, current)) {
                InventoryPresetStore.save(player, index)
            } else {
                confirm(
                    Component.translatable("screen.mc_tiny_tweak.inventory_presets.overwrite", index + 1),
                    Component.translatable("screen.mc_tiny_tweak.inventory_presets.overwrite.message"),
                ) { InventoryPresetStore.save(player, index) }
            }
        } else {
            val target = presets[index] ?: return
            val lost = InventoryPresets.lostSlots(current, target)
            if (lost.isEmpty()) {
                InventoryPresetStore.load(player, index)
            } else {
                confirm(
                    Component.translatable("screen.mc_tiny_tweak.inventory_presets.load", index + 1),
                    Component.translatable("screen.mc_tiny_tweak.inventory_presets.load.message", lost.size),
                ) { InventoryPresetStore.load(player, index) }
            }
        }
    }

    private fun confirm(title: Component, message: Component, action: () -> Unit) {
        minecraft.setScreen(
            ConfirmScreen(
                { confirmed ->
                    if (confirmed) action()
                    minecraft.setScreen(minecraft.player?.let(::InventoryScreen))
                },
                title,
                message,
            ),
        )
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        output.add(NarratedElementType.TITLE, message)
    }

    companion object {
        private const val WIDTH = 182
        private const val HEIGHT = 22
        private const val SLOT_STEP = 20
        private const val MAX_TOOLTIP_ITEMS = 12
        private val HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar")
        private val SELECTION_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_selection")

        /** Adds the bar to the survival inventory for players who may use `/item`. */
        fun register() {
            ScreenEvents.AFTER_INIT.register { client, screen, _, _ ->
                if (screen is InventoryScreen && ItemEditorSession.canApplyAsOperator(client)) {
                    Screens.getButtons(screen).add(InventoryPresetBar(screen))
                }
            }
        }
    }
}
