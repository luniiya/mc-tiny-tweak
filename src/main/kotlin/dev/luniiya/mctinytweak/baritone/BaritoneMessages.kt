package dev.luniiya.mctinytweak.baritone

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

/** Baritone's chat messages, as its logger hands them over: an empty root holding a `[Baritone]` tag, a space, then the text. */
object BaritoneMessages {
    private val TAG = Regex("""\[(Baritone|Baritoe|B)]""")

    /** The message without Baritone's leading tag, for a log where every line is Baritone's anyway. */
    fun withoutTag(message: Component): Component {
        val parts = message.siblings
        if (parts.size < 2 || !TAG.matches(parts[0].string) || parts[1].string != " ") return message
        val copy: MutableComponent = Component.empty().withStyle(message.style)
        for (part in parts.drop(2)) copy.append(part)
        return copy
    }
}
