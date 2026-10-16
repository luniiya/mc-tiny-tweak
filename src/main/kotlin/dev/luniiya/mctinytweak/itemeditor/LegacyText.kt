package dev.luniiya.mctinytweak.itemeditor

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import java.util.Optional

/**
 * Converts between editable `&`-code strings and styled components.
 *
 * Supported codes: `&0`-`&f` colors, `&#RRGGBB` hex colors, `&k&l&m&n&o` formats,
 * `&r` reset, and `&&` for a literal ampersand. Unknown codes are kept as text.
 * Parsed text is explicitly non-italic so item names and lore are not slanted by default.
 */
object LegacyText {
    private const val PREFIX = '&'
    private val BASE_STYLE: Style = Style.EMPTY.withItalic(false)

    fun parse(input: String): Component {
        val root = Component.empty().withStyle(BASE_STYLE)
        var style = BASE_STYLE
        val text = StringBuilder()

        fun flush() {
            if (text.isNotEmpty()) {
                root.append(Component.literal(text.toString()).withStyle(style))
                text.clear()
            }
        }

        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c != PREFIX || i + 1 >= input.length) {
                text.append(c)
                i++
                continue
            }
            val code = input[i + 1]
            if (code == PREFIX) {
                text.append(PREFIX)
                i += 2
                continue
            }
            if (code == '#' && i + 8 <= input.length) {
                val hex = input.substring(i + 2, i + 8)
                val rgb = hex.toIntOrNull(16)
                if (rgb != null && hex.all { it.isLetterOrDigit() }) {
                    flush()
                    style = BASE_STYLE.withColor(TextColor.fromRgb(rgb))
                    i += 8
                    continue
                }
            }
            val formatting = ChatFormatting.getByCode(code.lowercaseChar())
            if (formatting == null) {
                text.append(c)
                i++
                continue
            }
            flush()
            style = when {
                formatting == ChatFormatting.RESET -> BASE_STYLE
                formatting.isColor -> BASE_STYLE.withColor(formatting)
                else -> style.applyFormat(formatting)
            }
            i += 2
        }
        flush()
        return root
    }

    fun toLegacy(component: Component): String {
        val out = StringBuilder()
        var current = BASE_STYLE
        component.visit({ style, content ->
            val effective = normalize(style)
            if (content.isNotEmpty()) {
                if (effective != current) {
                    out.append(codesFor(effective))
                    current = effective
                }
                out.append(content.replace("$PREFIX", "$PREFIX$PREFIX"))
            }
            Optional.empty<Unit>()
        }, Style.EMPTY)
        return out.toString()
    }

    /** Keeps only the style properties representable as codes, treating unset italic as false. */
    private fun normalize(style: Style): Style {
        var result = BASE_STYLE
        style.color?.let { result = result.withColor(it) }
        if (style.isBold) result = result.withBold(true)
        if (style.isItalic) result = result.withItalic(true)
        if (style.isUnderlined) result = result.withUnderlined(true)
        if (style.isStrikethrough) result = result.withStrikethrough(true)
        if (style.isObfuscated) result = result.withObfuscated(true)
        return result
    }

    /** A color code or `&r` resets formats, so each change restates the whole style. */
    private fun codesFor(style: Style): String {
        val codes = StringBuilder()
        val color = style.color
        if (color != null) codes.append(colorCode(color)) else codes.append(PREFIX).append(ChatFormatting.RESET.char)
        if (style.isObfuscated) codes.append(PREFIX).append(ChatFormatting.OBFUSCATED.char)
        if (style.isBold) codes.append(PREFIX).append(ChatFormatting.BOLD.char)
        if (style.isStrikethrough) codes.append(PREFIX).append(ChatFormatting.STRIKETHROUGH.char)
        if (style.isUnderlined) codes.append(PREFIX).append(ChatFormatting.UNDERLINE.char)
        if (style.isItalic) codes.append(PREFIX).append(ChatFormatting.ITALIC.char)
        return codes.toString()
    }

    private fun colorCode(color: TextColor): String {
        val named = ChatFormatting.entries.firstOrNull { it.isColor && TextColor.fromLegacyFormat(it) == color }
        return if (named != null) "$PREFIX${named.char}" else "$PREFIX#%06X".format(color.value and 0xFFFFFF)
    }
}
