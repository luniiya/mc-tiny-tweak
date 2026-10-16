package dev.luniiya.mctinytweak.client

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.network.chat.contents.TranslatableContents

/** Hides the success messages of commands the mod sends in bulk, so chat does not flood. */
object HiddenCommandFeedback {
    /** How many more messages to hide, by translation key. */
    private val pending = HashMap<String, Int>()

    fun hide(keys: Collection<String>, count: Int) {
        if (count <= 0) return
        for (key in keys) pending.merge(key, count, Int::plus)
    }

    /** Drops what is left, so a message that never came does not hide a later one you asked for. */
    fun clear() = pending.clear()

    fun register() {
        ClientReceiveMessageEvents.ALLOW_GAME.register { message, overlay ->
            val key = (message.contents as? TranslatableContents)?.key
            val left = key?.let(pending::get) ?: 0
            if (overlay || left <= 0) return@register true
            if (left == 1) pending.remove(key) else pending[key!!] = left - 1
            false
        }
    }
}
