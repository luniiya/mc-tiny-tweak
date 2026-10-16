package dev.luniiya.mctinytweak.client.baritone

import baritone.api.BaritoneAPI
import dev.luniiya.mctinytweak.baritone.BaritoneMessages
import dev.luniiya.mctinytweak.client.mcp.Mcp
import dev.luniiya.mctinytweak.mcp.RecentLog
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import java.util.function.Consumer

/**
 * Baritone's messages, kept out of chat and shown in Mod menu > MCP > Baritone log instead. Agents still read them
 * through the MCP chat log, so the baritone tool keeps returning Baritone's replies.
 */
object BaritoneLog {
    const val CAPACITY = 500

    val lines = RecentLog<Component>(CAPACITY)

    /** Points Baritone's logger here. Only call when Baritone is installed. */
    fun install() {
        BaritoneAPI.getSettings().logger.value = Consumer(::add)
    }

    fun add(message: Component) {
        lines.add(BaritoneMessages.withoutTag(message))
        Mcp.chat.add(message.string)
    }

    /** Records a command someone ran, so the log shows what each reply answers. */
    fun command(text: String) {
        lines.add(Component.literal("> #$text").withStyle(ChatFormatting.AQUA))
    }
}
