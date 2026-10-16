package dev.luniiya.mctinytweak.client.keybinds

import com.mojang.blaze3d.platform.InputConstants
import dev.luniiya.mctinytweak.defaults.PersonalDefaults
import net.minecraft.ChatFormatting
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** Read-only list of the keybinds hardcoded in the mod's personal defaults, grouped like the Key Binds screen. */
class DefaultKeyBindsScreen(private val parent: Screen) : Screen(Component.translatable("controls.mc_tiny_tweak.view_defaults.title")) {
    private sealed interface Row
    private data class Header(val label: Component) : Row
    private data class Bind(val name: Component, val key: Component) : Row

    private val rows: List<Row> = buildRows()
    private var scroll = 0.0

    private fun buildRows(): List<Row> {
        val installed = mutableListOf<Pair<KeyMapping, String>>()
        val missing = mutableListOf<Pair<String, String>>()
        for ((name, keyName) in PersonalDefaults.keybinds) {
            val mapping = KeyMapping.get(name)
            if (mapping != null) installed += mapping to keyName else missing += name to keyName
        }
        val result = mutableListOf<Row>()
        var category: KeyMapping.Category? = null
        for ((mapping, keyName) in installed.sortedWith { a, b -> a.first.compareTo(b.first) }) {
            if (mapping.category != category) {
                category = mapping.category
                result += Header(mapping.category.label())
            }
            result += Bind(Component.translatable(mapping.name), keyLabel(keyName))
        }
        if (missing.isNotEmpty()) {
            result += Header(Component.translatable("controls.mc_tiny_tweak.view_defaults.not_installed"))
            for ((name, keyName) in missing.sortedBy { it.first }) {
                result += Bind(Component.literal(name).withStyle(ChatFormatting.GRAY), keyLabel(keyName))
            }
        }
        return result
    }

    private fun keyLabel(keyName: String): Component =
        runCatching { InputConstants.getKey(keyName) }.getOrNull()
            ?.takeIf { it != InputConstants.UNKNOWN }
            ?.displayName
            ?: Component.translatable("key.keyboard.unknown").withStyle(ChatFormatting.DARK_GRAY)

    private val listTop get() = 32
    private val listBottom get() = height - 36
    private val maxScroll get() = maxOf(0, rows.size * ROW_HEIGHT - (listBottom - listTop)).toDouble()

    override fun init() {
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(width / 2 - 100, height - 28, 200, 20).build())
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        scroll = (scroll - scrollY * ROW_HEIGHT * 2).coerceIn(0.0, maxScroll)
        return true
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF.toInt())
        val left = width / 2 - ROW_WIDTH / 2
        graphics.enableScissor(0, listTop, width, listBottom)
        rows.forEachIndexed { i, row ->
            val y = listTop + i * ROW_HEIGHT - scroll.toInt()
            if (y + ROW_HEIGHT < listTop || y > listBottom) return@forEachIndexed
            when (row) {
                is Header -> graphics.drawCenteredString(font, row.label, width / 2, y + 4, 0xFFFFD866.toInt())
                is Bind -> {
                    graphics.drawString(font, row.name, left, y + 4, 0xFFFFFFFF.toInt())
                    graphics.drawString(font, row.key, left + ROW_WIDTH - font.width(row.key), y + 4, 0xFFA0E0FF.toInt())
                }
            }
        }
        graphics.disableScissor()
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }

    private companion object {
        const val ROW_HEIGHT = 16
        const val ROW_WIDTH = 320
    }
}
