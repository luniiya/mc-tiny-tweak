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
 * Left click loads a preset, right click saves the current inventory into it, shift click renames it.
 * The mod's starter kit sits in a locked slot on the left, where the HUD shows the offhand.
 */
class InventoryPresetBar(private val screen: InventoryScreen) :
    AbstractWidget(0, 0, WIDTH, HEIGHT, Component.translatable("screen.mc_tiny_tweak.inventory_presets")) {
    private val minecraft = Minecraft.getInstance()

    init {
        place()
    }

    /** The bar sits exactly where the HUD hotbar is, with the kit slot where the HUD offhand slot is. */
    private fun place() {
        x = screen.width / 2 - BAR_WIDTH / 2 - KIT_WIDTH
        y = screen.height - HEIGHT
    }

    private val barX get() = x + KIT_WIDTH
    private val barY get() = y + 1

    /** The preset slot under the mouse, [KIT] for the starter kit, or -1. */
    private fun slotAt(mouseX: Double, mouseY: Double): Int {
        if (!isMouseOver(mouseX, mouseY)) return -1
        if (mouseX < barX) return if (mouseX < x + KIT_SPRITE_WIDTH) KIT else -1
        if (mouseY < barY) return -1
        return ((mouseX - barX - 1) / SLOT_STEP).toInt().coerceIn(0, InventoryPresets.SIZE - 1)
    }

    /** Decoding the kit needs the world's registries, so it is cached per world like the saved presets. */
    private var kit: Pair<Any, Map<String, ItemStack>?>? = null

    private fun kit(): Map<String, ItemStack>? {
        val registries = minecraft.player?.registryAccess() ?: return null
        kit?.takeIf { it.first === registries }?.let { return it.second }
        return minecraft.player?.let(InventoryPresetStore::starter).also { kit = registries to it }
    }

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        place()
        val player = minecraft.player ?: return
        val presets = InventoryPresetStore.presets(player)
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, KIT_SPRITE, x, y, KIT_SPRITE_WIDTH, HEIGHT)
        val kit = kit()
        kit?.let { graphics.renderItem(InventoryPresets(listOf(it)).icon(0), x + 3, y + 4) }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, barX, barY, BAR_WIDTH, BAR_HEIGHT)
        for (i in 0 until InventoryPresets.SIZE) {
            graphics.renderItem(presets.icon(i), barX + 3 + i * SLOT_STEP, barY + 3)
        }
        val hovered = slotAt(mouseX.toDouble(), mouseY.toDouble())
        if (hovered == KIT) {
            graphics.fill(x + 3, y + 4, x + 19, y + 20, 0x80FFFFFF.toInt())
            graphics.setTooltipForNextFrame(kitTooltip(kit), mouseX, mouseY)
        } else if (hovered >= 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SELECTION_SPRITE, barX - 1 + hovered * SLOT_STEP, barY - 1, 24, 23)
            graphics.setTooltipForNextFrame(tooltip(hovered, presets[hovered], presets.name(hovered)), mouseX, mouseY)
        }
    }

    private fun kitTooltip(kit: Map<String, ItemStack>?): List<FormattedCharSequence> {
        val lines = mutableListOf<Component>(Component.translatable("screen.mc_tiny_tweak.inventory_presets.kit"))
        kit?.let { items(it, lines) }
        lines += Component.translatable("screen.mc_tiny_tweak.inventory_presets.hint_load").withStyle(ChatFormatting.DARK_GRAY)
        lines += Component.translatable("screen.mc_tiny_tweak.inventory_presets.kit.locked").withStyle(ChatFormatting.DARK_GRAY)
        return lines.map { it.visualOrderText }
    }

    private fun items(slots: Map<String, ItemStack>, lines: MutableList<Component>) {
        val stacks = InventoryPresets.SLOT_NAMES.mapNotNull { slots[it] }
        stacks.take(MAX_TOOLTIP_ITEMS).forEach { stack ->
            lines += Component.literal("${stack.count}× ").withStyle(ChatFormatting.GRAY).append(stack.hoverName)
        }
        if (stacks.size > MAX_TOOLTIP_ITEMS) {
            lines += Component.translatable("screen.mc_tiny_tweak.inventory_presets.more", stacks.size - MAX_TOOLTIP_ITEMS)
                .withStyle(ChatFormatting.GRAY)
        }
    }

    private fun displayName(index: Int, name: String?): Component =
        name?.let(Component::literal) ?: Component.translatable("screen.mc_tiny_tweak.inventory_presets.slot", index + 1)

    private fun tooltip(index: Int, slots: Map<String, ItemStack>?, name: String?): List<FormattedCharSequence> {
        val lines = mutableListOf(displayName(index, name))
        if (slots == null) {
            lines += Component.translatable("screen.mc_tiny_tweak.inventory_presets.empty").withStyle(ChatFormatting.GRAY)
            lines += Component.translatable("screen.mc_tiny_tweak.inventory_presets.hint_save").withStyle(ChatFormatting.DARK_GRAY)
        } else {
            items(slots, lines)
            lines += Component.translatable("screen.mc_tiny_tweak.inventory_presets.hint_load").withStyle(ChatFormatting.DARK_GRAY)
            lines += Component.translatable("screen.mc_tiny_tweak.inventory_presets.hint_save_over").withStyle(ChatFormatting.DARK_GRAY)
            lines += Component.translatable("screen.mc_tiny_tweak.inventory_presets.hint_rename").withStyle(ChatFormatting.DARK_GRAY)
        }
        return lines.map { it.visualOrderText }
    }

    override fun isValidClickButton(info: MouseButtonInfo) = info.button() == 0 || info.button() == 1

    override fun onClick(event: MouseButtonEvent, doubleClick: Boolean) {
        val player = minecraft.player ?: return
        val index = slotAt(event.x(), event.y())
        if (index == KIT) {
            // The kit is the mod's: it loads, but nothing saves over or renames it.
            val kit = kit() ?: return
            if (event.button() == 0) load(Component.translatable("screen.mc_tiny_tweak.inventory_presets.kit"), kit) { InventoryPresetStore.give(player, kit) }
            return
        }
        if (index < 0) return
        val presets = InventoryPresetStore.presets(player)
        val existing = presets[index]
        when {
            event.hasShiftDown() && existing != null -> rename(index, presets.name(index))
            event.button() == 1 -> save(index, existing, presets.name(index))
            existing != null -> load(displayName(index, presets.name(index)), existing) { InventoryPresetStore.load(player, index) }
        }
    }

    /** Loads right away, or asks first when that would delete some of your stacks. */
    private fun load(name: Component, target: Map<String, ItemStack>, action: () -> Unit) {
        val player = minecraft.player ?: return
        val lost = InventoryPresets.lostSlots(InventoryPresetStore.snapshot(player), target).size
        if (lost == 0) action()
        else confirm(
            Component.translatable("screen.mc_tiny_tweak.inventory_presets.load", name),
            Component.translatable("screen.mc_tiny_tweak.inventory_presets.load.message", lost),
            action,
        )
    }

    private fun save(index: Int, existing: Map<String, ItemStack>?, name: String?) {
        val note = existing?.let { Component.translatable("screen.mc_tiny_tweak.inventory_presets.overwrite.message", displayName(index, name)) }
        minecraft.setScreen(
            InventoryPresetNameScreen(
                Component.translatable("screen.mc_tiny_tweak.inventory_presets.save", index + 1),
                name.orEmpty(),
                Component.translatable("screen.mc_tiny_tweak.inventory_presets.slot", index + 1),
                note,
                Component.translatable("screen.mc_tiny_tweak.inventory_presets.save.button"),
            ) { typed -> minecraft.player?.let { InventoryPresetStore.save(it, index, typed) } },
        )
    }

    private fun rename(index: Int, name: String?) {
        minecraft.setScreen(
            InventoryPresetNameScreen(
                Component.translatable("screen.mc_tiny_tweak.inventory_presets.rename", displayName(index, name)),
                name.orEmpty(),
                Component.translatable("screen.mc_tiny_tweak.inventory_presets.slot", index + 1),
                null,
                Component.translatable("screen.mc_tiny_tweak.inventory_presets.rename.button"),
            ) { typed -> minecraft.player?.let { InventoryPresetStore.rename(it, index, typed) } },
        )
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
        private const val KIT = -2
        private const val BAR_WIDTH = 182
        private const val BAR_HEIGHT = 22
        private const val KIT_SPRITE_WIDTH = 29
        /** The kit slot plus a gap, like the HUD offhand slot. */
        private const val KIT_WIDTH = 29
        private const val WIDTH = KIT_WIDTH + BAR_WIDTH
        private const val HEIGHT = 24
        private const val SLOT_STEP = 20
        private const val MAX_TOOLTIP_ITEMS = 12
        private val HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar")
        private val KIT_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_offhand_left")
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
