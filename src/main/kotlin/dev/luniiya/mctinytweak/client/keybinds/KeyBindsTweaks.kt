package dev.luniiya.mctinytweak.client.keybinds

import com.mojang.blaze3d.platform.InputConstants
import dev.luniiya.mctinytweak.defaults.PersonalDefaults
import dev.luniiya.mctinytweak.keybinds.KeyBindRules
import dev.luniiya.mctinytweak.keybinds.KeyBindRules.InputMode
import dev.luniiya.mctinytweak.mixin.client.keybinds.KeyBindsScreenAccessor
import dev.luniiya.mctinytweak.mixin.client.keybinds.KeyEntryAccessor
import net.minecraft.ChatFormatting
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.options.controls.KeyBindsList
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/**
 * Per-screen state for the Key Binds screen additions: a key search, a name or category search,
 * a tablet/mouse toggle, "unbind extras", and a read-only view of the personal defaults.
 * Search state survives the screen being re-initialized, for example after a resize.
 */
class KeyBindsTweaks(private val screen: KeyBindsScreen) {
    private val minecraft = Minecraft.getInstance()
    private val accessor = screen as KeyBindsScreenAccessor
    private val list: KeyBindsList get() = accessor.`mc_tiny_tweak$getKeyBindsList`()

    private var nameQuery = ""
    private var keyFilter: InputConstants.Key? = null
    private var listening = false
    private var entries: List<Pair<KeyMapping, KeyBindsList.Entry>> = emptyList()
    private var keySearchButton: Button? = null

    fun addHeader() {
        screen.layout.setHeaderHeight(HEADER_HEIGHT)
        val column = screen.layout.addToHeader(LinearLayout.vertical().spacing(6))
        column.defaultCellSetting().alignHorizontallyCenter()
        column.addChild(StringWidget(screen.title, minecraft.font))
        val row = column.addChild(LinearLayout.horizontal().spacing(8))

        keySearchButton = row.addChild(
            Button.builder(Component.empty()) { listening = true; refreshKeySearchLabel() }
                .width(SEARCH_WIDTH)
                .tooltip(Tooltip.create(Component.translatable("controls.mc_tiny_tweak.key_search.tooltip")))
                .build(),
        )
        refreshKeySearchLabel()

        val nameSearch = EditBox(minecraft.font, SEARCH_WIDTH, 20, Component.translatable("controls.mc_tiny_tweak.name_search"))
        nameSearch.value = nameQuery
        nameSearch.setHint(Component.translatable("controls.mc_tiny_tweak.name_search").withStyle(ChatFormatting.DARK_GRAY))
        nameSearch.setResponder {
            nameQuery = it
            applyFilter()
        }
        row.addChild(nameSearch)
    }

    /** Remembers the vanilla entries in their sorted order so filters can rebuild the list from them. */
    fun captureEntries() {
        entries = list.children().filterIsInstance<KeyBindsList.KeyEntry>()
            .map { (it as KeyEntryAccessor).`mc_tiny_tweak$getKey`() to it }
        applyFilter()
    }

    fun addFooter() {
        screen.layout.setFooterHeight(FOOTER_HEIGHT)
        val column = screen.layout.addToFooter(LinearLayout.vertical().spacing(4))
        column.defaultCellSetting().alignHorizontallyCenter()

        val tools = column.addChild(LinearLayout.horizontal().spacing(8))
        val options = minecraft.options
        tools.addChild(
            CycleButton.builder<InputMode>({ Component.translatable("controls.mc_tiny_tweak.mode.${it.name.lowercase()}") }, currentMode())
                .withValues(InputMode.entries)
                .withTooltip { Tooltip.create(Component.translatable("controls.mc_tiny_tweak.mode.${it.name.lowercase()}.tooltip")) }
                .create(0, 0, TOOL_WIDTH, 20, Component.translatable("controls.mc_tiny_tweak.mode")) { _, mode ->
                    options.keyAttack.setKey(InputConstants.getKey(mode.attack))
                    options.keyUse.setKey(InputConstants.getKey(mode.use))
                    refreshList()
                },
        )
        tools.addChild(
            Button.builder(Component.translatable("controls.mc_tiny_tweak.unbind_extras")) { confirmUnbindExtras() }
                .width(TOOL_WIDTH)
                .tooltip(Tooltip.create(Component.translatable("controls.mc_tiny_tweak.unbind_extras.tooltip")))
                .build(),
        )
        tools.addChild(
            Button.builder(Component.translatable("controls.mc_tiny_tweak.view_defaults")) {
                minecraft.setScreen(DefaultKeyBindsScreen(screen))
            }.width(TOOL_WIDTH).build(),
        )

        val main = column.addChild(LinearLayout.horizontal().spacing(8))
        val reset = Button.builder(Component.translatable("controls.resetAll")) {
            for (mapping in options.keyMappings) mapping.setKey(mapping.defaultKey)
            refreshList()
        }.build()
        accessor.`mc_tiny_tweak$setResetButton`(reset)
        main.addChild(reset)
        main.addChild(Button.builder(CommonComponents.GUI_DONE) { screen.onClose() }.build())
    }

    /** Captures the next key while the key search is listening. Escape clears the filter. */
    fun onKeyPressed(event: KeyEvent): Boolean {
        if (!listening || screen.selectedKey != null) return false
        setKeyFilter(if (event.isEscape) null else InputConstants.getKey(event))
        return true
    }

    /** Captures the next mouse button while the key search is listening. */
    fun onMouseClicked(event: MouseButtonEvent): Boolean {
        if (!listening || screen.selectedKey != null) return false
        setKeyFilter(InputConstants.Type.MOUSE.getOrCreate(event.button()))
        return true
    }

    private fun setKeyFilter(key: InputConstants.Key?) {
        keyFilter = key
        listening = false
        refreshKeySearchLabel()
        applyFilter()
    }

    private fun refreshKeySearchLabel() {
        val button = keySearchButton ?: return
        val filter = keyFilter
        button.message = when {
            listening -> Component.literal("> ").append(Component.translatable("controls.mc_tiny_tweak.key_search.listening")).append(" <")
                .withStyle(ChatFormatting.YELLOW)
            filter != null -> Component.translatable("controls.mc_tiny_tweak.key_search.active", filter.displayName)
            else -> Component.translatable("controls.mc_tiny_tweak.key_search")
        }
    }

    private fun applyFilter() {
        if (entries.isEmpty()) return
        val filtered = mutableListOf<KeyBindsList.Entry>()
        var category: KeyMapping.Category? = null
        for ((mapping, entry) in entries) {
            val name = Component.translatable(mapping.name).string
            if (!KeyBindRules.matchesName(nameQuery, name, mapping.category.label().string)) continue
            if (!KeyBindRules.matchesKey(keyFilter?.name, mapping.saveString())) continue
            if (mapping.category != category) {
                category = mapping.category
                filtered += list.CategoryEntry(mapping.category)
            }
            filtered += entry
        }
        list.replaceEntries(filtered)
        list.setScrollAmount(0.0)
    }

    private fun refreshList() {
        list.resetMappingAndUpdateButtons()
        applyFilter()
    }

    private fun currentMode() = InputMode.detect(minecraft.options.keyAttack.saveString(), minecraft.options.keyUse.saveString())

    private fun confirmUnbindExtras() {
        val extras = KeyBindRules.extras(minecraft.options.keyMappings.map { it.name }, PersonalDefaults.keybinds).toSet()
        minecraft.setScreen(
            ConfirmScreen(
                { confirmed ->
                    if (confirmed) {
                        minecraft.options.keyMappings.filter { it.name in extras }.forEach { it.setKey(InputConstants.UNKNOWN) }
                        refreshList()
                        minecraft.options.save()
                    }
                    minecraft.setScreen(screen)
                },
                Component.translatable("controls.mc_tiny_tweak.unbind_extras"),
                Component.translatable("controls.mc_tiny_tweak.unbind_extras.confirm", extras.size),
            ),
        )
    }

    private companion object {
        const val HEADER_HEIGHT = 58
        const val FOOTER_HEIGHT = 58
        const val SEARCH_WIDTH = 150
        const val TOOL_WIDTH = 110
    }
}
