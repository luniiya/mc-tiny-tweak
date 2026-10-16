package dev.luniiya.mctinytweak.client

import dev.luniiya.mctinytweak.notify.NoticeStyle
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

/** Shows the mod's own chat notifications. They are local only and never reach the agent's chat log. */
object Notices {
    /** True while a notice is being added, so the chat log can skip it. */
    @JvmStatic
    var showing = false
        private set

    /** Safe from any thread; the line is added on the client thread. */
    fun show(text: Component) {
        val minecraft = Minecraft.getInstance()
        minecraft.execute {
            val line = Component.literal(NoticeStyle.TAG).withStyle(Style.EMPTY.withColor(NoticeStyle.TAG_COLOR))
                .append(Component.literal(" "))
                .append(text.copy().withStyle(Style.EMPTY.withColor(NoticeStyle.TEXT_COLOR)))
            showing = true
            try {
                minecraft.gui.chat.addMessage(line)
            } finally {
                showing = false
            }
        }
    }

    fun show(text: String) = show(Component.literal(text))
}
